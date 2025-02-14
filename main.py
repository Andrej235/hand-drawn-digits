from collections import namedtuple

import numpy as np;

Layer = namedtuple('Layer', ['weights', 'biases'])

layerSizes = [2, 3, 5]
layers = np.zeros([len(layerSizes), 2])

def init_layers():
    weights = [];
    biases = [];
    rng = np.random.default_rng(seed=1)
    for i in range(len(layerSizes) - 1):
        weights.append(rng.standard_normal((layerSizes[i + 1], layerSizes[i])))
        biases.append(rng.standard_normal(layerSizes[i + 1]))

    return weights, biases

def feed_forward(input, l):
    if l == len(layerSizes) - 1:
        return input
    
    output = sigmoid(np.add(np.dot(weights[l], input), biases[l]))
    return feed_forward(output, l + 1)

def sigmoid(x):
    return np.divide(1, np.add(1, np.exp(-x)))

weights, biases = init_layers()
output = feed_forward(np.array([0., 1.]), 0)
print(output)