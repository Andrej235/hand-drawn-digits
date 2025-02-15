import numpy as np;

class NeuralNetwork:
    def __init__(self, layer_sizes, seed=1):
        self.layer_sizes = layer_sizes
        self.weights, self.biases = self.init_layers(seed)
        self.z_values = []
        self.activations = []

    def init_layers(self, seed):
        weights = []
        biases = []
        rng = np.random.default_rng(seed)
        for i in range(len(self.layer_sizes) - 1):
            weights.append(rng.standard_normal((self.layer_sizes[i + 1], self.layer_sizes[i])))
            biases.append(rng.standard_normal(self.layer_sizes[i + 1]))
        return weights, biases

    def sigmoid(self, x):
        return 1 / (1 + np.exp(-x))

    def sigmoid_prime(self, x):
        return self.sigmoid(x) * (1 - self.sigmoid(x))
    
    def calculate_cost(self, real_output, expected_output):
        return np.mean((real_output - expected_output) ** 2)    
    
    def forward(self, input):
        self.z_values = []
        self.activations = [input]
        current_activation = input
        
        for weights, biases in zip(self.weights, self.biases):
            z = np.dot(weights, current_activation) + biases
            current_activation = self.sigmoid(z)
            self.z_values.append(z)
            self.activations.append(current_activation)
            
        return current_activation
    
    def backward(self, expected_output):
        weight_gradient = [np.zeros(w.shape) for w in self.weights]
        bias_gradient = [np.zeros(b.shape) for b in self.biases]
        
        base = self.sigmoid_prime(self.z_values[-1]) * 2 * (self.activations[-1] - expected_output)
        bias_gradient[-1] = base
        weight_gradient[-1] = np.outer(base, self.activations[-2])
        
        for l in range(len(self.weights) - 2, -1, -1):
            base = np.dot(base, self.weights[l + 1]) * self.sigmoid_prime(self.z_values[l])
            bias_gradient[l] = base
            weight_gradient[l] = np.outer(base, self.activations[l])
            
        for i in range(len(self.weights)):
            print(f"Weight {i + 1}:")
            print(self.weights[i].shape)
            print(weight_gradient[i].shape)