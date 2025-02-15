import numpy as np
from nn import NeuralNetwork

nn = NeuralNetwork(layer_sizes=[2, 3, 5])
input_data = np.array([0.0, 1.0])
output = nn.forward(input_data)
target = np.array([0, 0, 0, 1, 0])
print(nn.calculate_cost(output, target))
nn.backward(target)