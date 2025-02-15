import numpy as np
from nn import NeuralNetwork

nn = NeuralNetwork(layer_sizes=[28*28, 50, 10])
nn.train(100, .1)
nn.save_model('model_100epoch.npz')


# NeuralNetwork.load_model("model_100epoch.npz").test()     96.18%
# NeuralNetwork.load_model("model.npz").test()              96.27%