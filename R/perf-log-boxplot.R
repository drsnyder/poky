# Rscript perf-log-boxplot.R pre-poky-perf post-poky-perf "Title"
# sed 's/.*INFO: //' ocn-forum-thread-perf.log.20130701.6 > ocn-forum-thread-perf.log.20130701.6.csv
require(reshape2)
require(ggplot2)

args = commandArgs(TRUE)
pre = read.csv(args[1], header=F, sep=",")
post = read.csv(args[2], header=F)

# PHP time V3
# Poky time V10
# Poky multi time V12

# limit the sample size
sample_size = 200000
pre_sample_size = min(sample_size, length(pre$V3))
inclusive_pre = pre$V3 + pre$V10 + pre$V12
prephp = sort(inclusive_pre[sample(length(inclusive_pre), size=pre_sample_size, replace=F)])

post_sample_size = min(sample_size, length(post$V3))
inclusive_post = post$V3 + post$V10 + post$V12
postphp = sort(inclusive_post[sample(length(inclusive_post), size=post_sample_size, replace=F)])

png("tmp/before-after.png")
boxplot(prephp, postphp, outline=F, col=c("lightgreen", "lightblue"),
  xlab="PHP Time", ylab="Seconds")
axis(side=1, 1:2, labels=c("Pre Poky", "With Poky"), cex.axis=0.8)
title(args[3])
dev.off()

t.test(prephp, postphp, conf.level=0.99)

qs = c(0.05, 0.25, 0.5, 0.75, 0.95)
print("Pre quantiles:")
preq = quantile(prephp, probs = qs)
print(preq)
print("Post quantiles:")
postq = quantile(postphp, probs = qs)
print(postq)

preqdf = data.frame(id = qs, values = preq, name="Pre")
postqdf = data.frame(id = qs, values = postq, name="Post")
d = rbind(preqdf, postqdf)
ggplot(d, aes(x=factor(id), y=values, fill=name)) +
  geom_bar(position="dodge", stat="identity") +
  labs(y="Time", x="Quantile", title=paste(args[3], "Pre and Post Quantiles")) +
  theme(legend.title=element_blank())
ggsave("tmp/before-after-quantiles.png")

rmin = min(prephp, postphp)
# trim the outliers at the 99th %
rmax = max(prephp[0.99*length(prephp)], postphp[0.99*length(postphp)])
x = seq(rmin,rmax,by=0.001)
prephpecdf = ecdf(prephp)
postphpecdf = ecdf(postphp)
df = data.frame(cbind(x=x, pre=prephpecdf(x), post=postphpecdf(x)))

ggplot(df, aes(x=x)) + 
  geom_line(aes(y=pre,color="Pre Poky")) +
  geom_line(aes(y=post, color="Post Poky")) +
  labs(y="Probability", x="Time", title=paste(args[3], "CDF of pre and post poky PHP time")) +
  theme(legend.title=element_blank())
ggsave("tmp/before-after-cdf.png")

