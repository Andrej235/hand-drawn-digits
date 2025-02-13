from collections import namedtuple

import numpy as np;

Layer = namedtuple('Layer', ['weights', 'biases'])

layerSizes = [2, 3, 2]
layers = np.zeros([len(layerSizes), 2])

def init_layers():
    layers = [];
    rng = np.random.default_rng(seed=1)
    for i in range(len(layerSizes) - 1):
        layers += Layer(rng.standard_normal((layerSizes[i], layerSizes[i + 1])), rng.standard_normal(layerSizes[i + 1]))

    return layers

layers = np.array(init_layers(), dtype=Layer)
print(layers)