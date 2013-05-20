###
# Most of the default settings below should be fine. Under normal
# circumstances, only the hostnames need to be changed to match a given
# environment.
#
# Settings:
# -s branch=http-head 
# -s jmx_port=9191
# -s statsd_host=""
# -s varnishd="/usr/local/sbin/varnishd"
# -s varnish_listen_address=""
# -s varnish_listen_port=8080
# -s varnish_storage_size="512M"
# -s database_url="postgresql://postgres@host:port/poky"
#
# Behavior:
# -s poky_skip_start=false
# -s poky_skip_stop=false
# -s varnish_skip_start=false
# -s varnish_skip_stop=false
#
###
set :application, "Poky"
set :repository,  "git@github.com:drsnyder/poky.git"

set :scm, :git
set :deploy_via, :copy
set :deploy_to, "/var/www/poky"
set :user, "deployer"
set :group, "deployer"
set :use_sudo, false

deploy_env = {}

# see config/poky.defaults
deploy_env['POKY_PORT']  = 8081
deploy_env['JMX_PORT']   = fetch(:jmx_port, 9191)
deploy_env['STATSD_HOST'] = fetch(:statsd_host, "")

# see config/varnish.defaults 
deploy_env['VARNISHD'] = fetch(:varnishd, "/usr/local/sbin/varnishd")
deploy_env['VARNISH_LISTEN_ADDRESS'] = fetch(:varnish_listen_address, "")
deploy_env['VARNISH_LISTEN_PORT'] = fetch(:varnish_listen_port, 8080)

# amount of storage to supply to varnish in bytes. use k / M / G suffix
deploy_env['VARNISH_STORAGE_SIZE'] = fetch(:varnish_storage_size, "512M")

desc "Deploy to the poky development environment."
task :development do
    deploy_env["DATABASE_URL"] = fetch(:database_url,"postgresql://postgres@dev.poky.huddler.com/poky")
    role :app, "dev.poky.huddler.com"
end

desc "Deploy to the poky qa environment."
task :qa do
    deploy_env["DATABASE_URL"] = fetch(:database_url,"postgresql://postgres@qa.poky.huddler.com/poky")
    role :app, "qa.poky.huddler.com"
end

desc "Deploy to the poky production environment."
task :production do
    deploy_env["DATABASE_URL"] = fetch(:database_url,"postgresql://postgres@poky.huddler.com/poky")
    role :app, "poky.huddler.com"
end


namespace :poky do

    desc "Stop the poky HTTP service."
    task :stop, :roles => :app do
        run "#{current_release}/scripts/poky.init.sh stop" unless fetch(:poky_skip_stop, false) or fetch(:skip_stop, false)
    end

    desc "Start the poky HTTP service."
    task :start, :roles => :app do
        run "#{release_path}/scripts/poky.init.sh start" unless fetch(:poky_skip_start, false)
    end


    desc "Check that the poky service is up and running."
    task :check, :roles => :app do
        run "curl http://localhost:#{deploy_env['POKY_PORT']}/status"
    end 

end

namespace :varnish do

    desc "Stop varnish."
    task :stop, :roles => :app do
        run "#{current_release}/scripts/varnish.init.sh stop" unless fetch(:varnish_skip_stop, false) or fetch(:skip_stop, false)
    end

    desc "Start varnish."
    task :start, :roles => :app do
        run "#{release_path}/scripts/varnish.init.sh start" unless fetch(:varnish_skip_start, false)
    end


    desc "Check that varnish is up and running."
    task :check, :roles => :app do
        run "curl http://#{deploy_env['VARNISH_LISTEN_ADDRESS'].empty? ? "localhost" : deploy_env['VARNISH_LISTEN_ADDRESS']}:#{deploy_env['VARNISH_LISTEN_PORT']}/status"
    end 

    desc "Reload the varnish config."
    task :reload, :roles => :app do
        run "#{current_release}/scripts/varnishreload"
    end

end

namespace :deploy do

    # allow -s branch=todeploy
    set :branch, fetch(:branch, "http-head")

    # allow -s build_script="/path/to/lein uberjar"
    set :build_script, fetch(:build_script, "lein clean && lein uberjar")

    task :stop, :roles => :app do
        poky.stop
        varnish.stop
    end

    task :start, :roles => :app do
        poky.start
        varnish.start
    end

    task :restart, :roles => :app do
        poky.stop
        poky.start
        varnish.stop
        varnish.start
    end


    task :finalize_update do
        deploy_env.keys.each do |k|
            run "echo 'export #{k}=#{deploy_env[k]}' >> #{release_path}/config/environment"
        end
    end

    task :migrate do 
        # noop
    end

    desc "Set the permissions on /var/www/poky. This enables deploy:setup."
    task :set_perms do
        sudo "mkdir -p /var/www/poky > /dev/null 2>&1 || true"
        sudo "chown #{user}:#{group} /var/www/poky"
    end


    task :update_symlinks do
        run "ln -s #{shared_path}/log #{release_path}/log"
        run "mkdir #{release_path}/tmp"
        run "ln -s #{shared_path}/pids #{release_path}/tmp/pids"
    end

    task :pause do
        puts 
        puts "Waiting for poky to come up..."
        puts
        sleep 5
    end



end

after  "deploy:create_symlink",  "deploy:update_symlinks"
after  "deploy:update_symlinks", "deploy:migrate"
after  "deploy",                 "deploy:pause"
after  "deploy",                 "poky:check"
after  "deploy",                 "varnish:check"
after  "deploy",                 "varnish:reload"
after  "deploy",                 "deploy:cleanup"

before "deploy:setup",           "deploy:set_perms"
after  "varnish:stop",           "deploy:pause"
after  "poky:stop",              "deploy:pause"
