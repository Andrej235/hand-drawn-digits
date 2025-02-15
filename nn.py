import numpy as np;
import mnist

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
        with np.errstate(over='ignore', divide='ignore'):
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
            
        return weight_gradient, bias_gradient
            
    def load_mnist(self, filename):
        with open(filename, 'rb') as fd:
            return mnist.parse_idx(fd)
    
    def train(self, epochs, learning_rate):
        inputs = self.load_mnist('./mnist/train-images.idx3-ubyte').reshape(60000, 784).astype(np.float32) / 255.0
        expected_outputs = np.eye(10)[self.load_mnist('./mnist/train-labels.idx1-ubyte')]
        
        rng = np.random.default_rng(1)
        num_samples = inputs.shape[0]
        for epoch in range(epochs):
            total_cost = 0
            
            indices = np.arange(num_samples)
            rng.shuffle(indices)

            for i in indices:
                x = inputs[i]
                y_expected = expected_outputs[i]

                output = self.forward(x)
                
                cost = self.calculate_cost(output, y_expected)
                total_cost += cost

                weight_gradient, bias_gradient = self.backward(y_expected)

                for l in range(len(self.weights)):
                    self.weights[l] -= learning_rate * weight_gradient[l]
                    self.biases[l] -= learning_rate * bias_gradient[l]

            avg_cost = total_cost / num_samples
            print(f"Epoch {epoch+1}/{epochs} - Average Cost: {avg_cost:.4f}")
        
    def save_model(self, filename):
        save_dict = {
            "layer_sizes": np.array(self.layer_sizes),
        }
        
        for i, (w, b) in enumerate(zip(self.weights, self.biases)):
            save_dict[f"weights_{i}"] = w
            save_dict[f"biases_{i}"] = b
        
        np.savez_compressed(filename, **save_dict)
        
    @staticmethod
    def load_model(filename):
        data = np.load(filename, allow_pickle=True)
        layer_sizes = data["layer_sizes"].tolist()
        network = NeuralNetwork(layer_sizes)
        
        network.weights = []
        network.biases = []
        num_layers = len(layer_sizes) - 1
        
        for i in range(num_layers):
            network.weights.append(data[f"weights_{i}"].astype(np.float32))
            network.biases.append(data[f"biases_{i}"].astype(np.float32))
        
        return network
    
    def test(self):
        inputs = self.load_mnist('./mnist/t10k-images.idx3-ubyte').reshape(10000, 784).astype(np.float32) / 255.0
        expected_outputs = np.eye(10)[self.load_mnist('./mnist/t10k-labels.idx1-ubyte')]

        total_samples = inputs.shape[0]
        correct_count = 0
        
        for i in range(total_samples):
            x = inputs[i]
            y_expected = expected_outputs[i]

            output = self.forward(x)
            y_prediction = np.argmax(output)

            if y_prediction == np.argmax(y_expected):
                correct_count += 1
                
        print(f"Accuracy: {correct_count / total_samples * 100:.2f}% ({correct_count}/{total_samples})")
