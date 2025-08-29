import matplotlib.pyplot as plt
import numpy as np



if __name__ == "__main__":
    
    a: np.ndarray = np.arange(25).reshape(5, 5)
    
    
    # Example usage
    fig = plt.figure()
    ax = fig.add_subplot(111, projection='3d')
    ax.set_xlim([0, 10])
    ax.set_ylim([0, 10])
    ax.set_zlim([0, 10])
    plt.show()