##Set working directory

setwd("./")

##Load the the packages required to predict

library(caret)
library(e1071)

##Load de predictor model and the featured vector

load("model.Rda")

microserviciosTest <- read.csv("FeatureVector.csv")

##Predict the results

microserviciosPredictionKNN <- predict(DTFit3, microserviciosTest)

##Save the results

write.table(microserviciosPredictionKNN, file="microservicePrediction.txt", row.names = FALSE, col.names = FALSE)
