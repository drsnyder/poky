# Read in perf logs and generate summary information.
require(ggplot2)
args <- commandArgs(TRUE)

datafile <- readLines(args[1])
csv <- read.csv(text=sub('.*INFO: ', "", datafile, perl=T), header=F)

# total
print("Total time:")
summary(csv$V2)

# php
print("PHP time:")
summary(csv$V3)

# percetiles
p <- sort(csv$V3)
print(paste("95%: ", p[0.95*length(p)]))
