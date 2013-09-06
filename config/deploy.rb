###
# Most of the default settings below should be fine. Under normal
# circumstances, only the hostnames need to be changed to match a given
# environment.
#
# Settings:
# -s branch=master
# -s jmx_port=9101
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
set :use_varnish, false

deploy_env = {}

task :setup do
  # see config/poky.defaults
  deploy_env['POKY_PORT']  = fetch(:poky_port, 9091); # if you change here, change in default.vcl
  deploy_env['JMX_PORT']   = fetch(:jmx_port, 9101)
  deploy_env["STATSD_HOST"] = fetch(:statsd_host, "utility002:8125")
  deploy_env["STATSD_KEY_BASE"] = fetch(:statsd_key_base, "poky")

  # see config/varnish.defaults
  deploy_env['VARNISHD'] = fetch(:varnishd, "/usr/local/sbin/varnishd")
  deploy_env['VARNISH_LISTEN_ADDRESS'] = fetch(:varnish_listen_address, "")
  deploy_env['VARNISH_LISTEN_PORT'] = fetch(:varnish_listen_port, 8080)

  # amount of storage to supply to varnish in bytes. use k / M / G suffix
  deploy_env['VARNISH_STORAGE_SIZE'] = fetch(:varnish_storage_size, "1024M")

  deploy_env["VARNISH_PURGE_HOST"] = fetch(:varnish_purge_host, "localhost")
  deploy_env["VARNISH_PURGE_PORT"] = deploy_env['VARNISH_LISTEN_PORT']
end


desc "Deploy to the poky development environment."
task :development do
  setup
  host = "dev.poky.huddler.com"
  role :app, host
  deploy_env["VARNISH_PURGE_HOST"] = host
  deploy_env["DATABASE_URL"] = fetch(:database_url,"postgresql://postgres@#{host}/poky_dev")
  deploy_env["STATSD_KEY_BASE"] = "poky.dev"
end

desc "Deploy to the poky qa environment."
task :qa do
  setup
  host = "qa.poky.huddler.com"
  role :app, host
  deploy_env["VARNISH_PURGE_HOST"] = host
  deploy_env["DATABASE_URL"] = fetch(:database_url,"postgresql://postgres@#{host}/poky_qa")
  deploy_env["STATSD_KEY_BASE"] = "poky.qa"
end

desc "Deploy to the poky production environment."
task :production do
  setup
  host = "prod.poky.huddler.com"
  role :app, host
  deploy_env["VARNISH_PURGE_HOST"] = host
  deploy_env["DATABASE_URL"] = fetch(:database_url,"postgresql://postgres@#{host}/poky")
  deploy_env["STATSD_HOST"] = "utility002-private:8125"
  deploy_env["VARNISH_STORAGE_SIZE"] = "8192M"
  deploy_env["VARNISH_MAX_THREADS"] = "2000"
  deploy_env["VARNISH_THREAD_POOLS"] = "8"
  deploy_env["STATSD_KEY_BASE"] = "poky.prod"
  deploy_env["NEWRELIC_AGENT"] = "-javaagent:/var/lib/newrelic/newrelic.jar"
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
    run "curl -s http://localhost:#{deploy_env['POKY_PORT']}/status"
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
    # if VARNISH_LISTEN_ADDRESS is empty, use localhost. in this case, any
    # interface will do
    listen = deploy_env['VARNISH_LISTEN_ADDRESS'].empty? ? "localhost" : deploy_env['VARNISH_LISTEN_ADDRESS']
    run "curl -s http://#{listen}:#{deploy_env['VARNISH_LISTEN_PORT']}/status"
  end 

  desc "Reload the varnish config."
  task :reload, :roles => :app do
    run "#{current_release}/scripts/varnishreload"
  end

end

namespace :deploy do

  # allow -s branch=todeploy
  set :branch, fetch(:branch, "master")

  # allow -s build_script="/path/to/lein uberjar"
  set :build_script, fetch(:build_script, "lein clean && lein uberjar")

  task :stop, :roles => :app do
    poky.stop
    varnish.stop if fetch(:use_varnish, false)
  end

  task :start, :roles => :app do
    poky.start
    varnish.start if fetch(:use_varnish, false)
  end

  task :restart, :roles => :app do
    poky.stop
    poky.start

    if fetch(:use_varnish, false)
      varnish.stop
      varnish.start
    end
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
    run "[ ! -d #{shared_path}/run ] && mkdir #{shared_path}/run || true"
    run "mkdir #{release_path}/tmp"
    run "ln -s #{shared_path}/log #{release_path}/log"
    run "ln -s #{shared_path}/run #{release_path}/run"
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
after  "deploy",                 "deploy:cleanup"

before "deploy:setup",           "deploy:set_perms"
after  "poky:stop",              "deploy:pause"

if fetch(:use_varnish, false)
  after  "varnish:stop",           "deploy:pause"
  after  "deploy",                 "varnish:check"
  after  "deploy",                 "varnish:reload"
end
