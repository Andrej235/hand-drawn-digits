import numpy as np
from nn import NeuralNetwork

nn = NeuralNetwork(layer_sizes=[28*28, 50, 10])
nn.train(10, 0.1)