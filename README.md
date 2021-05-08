# Segmentation Using Saturation Thresholding

The overal goal of this project is to replicate the results of the paper "Segmentation Using Saturation Thresholding and Its Application in Content-Based Retrieval of Images".
https://link.springer.com/chapter/10.1007/978-3-540-30125-7_5

Planned features are:
- Implement Lloyd's algorithm.
  - Works on a general multi-dimensional dataset.
  - Multi-threaded.
  - Robust selection of initial cluster centers.
- Implement ISODATA. Same requirements as Lloyd's algorithm.
- Support large datasets.
- Cluster data points based on saturation as described in the above mentioned paper.
- Implement some kind of circular clustering, like circular Otsu's method.
- Compare results, and supervised evaluation.

API docs: https://zooflavor.github.io/ttaf/javadoc/index.html
