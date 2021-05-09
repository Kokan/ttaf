# Segmentation Using Saturation Thresholding

## Intro

The overall goal of this project is to replicate the results of the paper
"Segmentation Using Saturation Thresholding and Its Application in Content-Based Retrieval of Images"[[1]](#1).
The paper postulates that separately clustering grayish pixels by their intensity
and colorful pixels by their hue produces better results than clustering pixels by their RGB values.

To be able to compare the results of various clustering strategies we implemented
- a framework to efficiently process large hyper-spectral images
- clustering algorithms
  - LLoyd's algorithm
  - elbow method
  - ISODATA[[2]](#2)
  - Otsu's method[[3]](#3)
  - circular Otsu's method[[4]](#4)
  - saturation based clustering
- image transformations
  - extracting color components
  - intensity
  - hue and hyper-spectral hue[[5]](#5)
  - normalization of input.

## Clustering algorithms

### LLoyd's algorithm

This is also known as k-means. Lloyd's algorithm is an iterative method for any-dimensional euclidean spaces.

The parameters of k-means are
- clusters: the number of classes the data points are separated into
- errorLimit: early stopping when the clustering error fails to decrease as fast as the errorLimit rate
- maxIterations: the maximum number of classify-recenter iteration the algorithm will
  use before accepting the current clusters
- initial center selection: there are three strategies to select the initial cluster centers
  - randomly select data points
  - select the mean of the data points as the first center
    and for the rest select one of the data points which is farthest from all the selected centers
  - form a kd-tree over the data points and select the means of the partitions defined by the tree 
- replacement center selection: during the execution of k-means clusters may become empty, and must be replaced by
  - randomly selecting a data point which is not already a cluster center
  - selecting one of the data points which is the farthest from existing centers.

### ISODATA

ISODATA is an improvement of k-means which doesn't require the user to give the correct number of clusters up front.

Some parameters of ISODATA are the same as k-means:
- errorLimit
- maxIterations
- initial centers selection
- replacement center selection.

Other parameters are:
- minClusters and maxClusters: the range of the number clusters the result can be chosen from
- foo
- bar
- baz.

### Otsu's method

Otsu's method exhaustively searches for the optimal thresholding of the one-dimensional histogram of the input data.
The circular version also considers all rotations of the histogram.

Its parameters are:
- bins: the number of bins of the histogram, the range of the histogram is the same as the true range of the input data
- clusters: the number of classes the data points are separated into

### Elbow method

The elbow method can transform a clustering algorithms which works on a fixed number of cluster to one which
searches a range of possible numbers of cluster
and selects the one where further increasing the number of clusters will not pay off.


The parameters of the elbow method are:
- errorLimit: stops when the clustering error fails to decrease as fast as the errorLimit rate
- minClusters and maxClusters: the range of the number clusters the result can be chosen from

### Saturation based clustering

Saturation based clustering considers the grayish and colorful pixels of an image separately,
the two classes of pixels are clustered independently of each other.
In the output image all pixels will be assigned a new color depending on which cluster they belong to.
Grayish pixel will have various intensity of gray colors.
Colorful pixel will have fully saturated colors with various hues.
The separation is pixels is based on the saturation of a pixel.

Its parameters are:
- a clustering strategy used for both classes
- type of the color model used
  - hue: RGB images are converted to HSV and gray pixels will be those with low saturation,
    gray values are the value component, color values are the hue component
  - hyper-hue: work for any color space having at least 2 components,
    pixels are decomposed into a gray part parallel to (1, 1, 1, ...) and a color part perpendicular to it,
    pixels are considered gray if the gray part is longer than the color part.

## Image transformations

An image transform reads an input image and produces a new one. Transforms can be composed.

### Clustering

Clustering can be used as an image transformation.
Color can be assigned to each cluster and all pixels can be replaced with the color of its nearest center.

### Extracting color components

The components of the input image can be rearranged, dropped, or duplicated.

### Intensity

This transform produces a new image where every pixel is replaced with its euclidean length.

### Hue

This transformation takes the first 3 component of the image as RGB values, converts it to HSV,
and selects the hue component.

### Hyper-spectral hue

Hyper-spectral hue of a pixel is defined to be the pixel's projection to the hyperplane
perpendicular to the vector (1, 1, 1, ...).
It can be defined also as the removal of the gray (1, 1, 1, ....) component.

### Normalization of input

Normalization is component-wise. There are two kind of normalization implemented.

#### Range based

Maps input values from their true range to the range of the underlying storage.
This is mostly for human consumption.

#### Deviation based

Calculates the statistical mean and deviation of the input values,
and maps the range `[mean - sigma * deviation, mean + sigma * deviation]` to the range of the underlying storage.
Values outside the range are clipped.
This is useful to discard outliers.
Sigma is a parameter of the transformation. 

## Library

The project as a library has 4 parts:
- containers for pixel data
- continuation passing style multi-threading
- clustering algorithms
- image manipulation

### Containers

The container classes are in the package `dog.giraffe.points`. Points as in data points.

The basic type is `Vector` which is a collection of coordinate values.
Every pixel is thought of as a vector in a euclidean space, every component is mapped to a dimension.

`Points` are collections of vectors.
`Points` are used by clustering algorithms which only inspects input data.
`Points` doesn't have to store every vector in a `Vector` instance, and most of them don't.

`MutablePoints` can be modified, vectors can be added, removed, modified, and rearranged.
`MutablePoints` are used to manipulate images.

`Points` have methods to manipulate individual vectors and group operations.
These group operations are defined using the individual operations,
but subclasses are expected to provide more efficient implementations.
The three most commonly used group operations are summing up all vectors, assigning the nearest center to all vectors,
and copying a range of vectors between two points.

There are two main type of `Points`.
The array based mutable points store all vectors in one array of some primitive type.
There are implementations for bytes, shorts and floats.
These are used to read, transform and write images.

`KDTree` takes a mutable points, rearranges the order of vectors, and forms a kd-tree over all the vectors.
This can speed up the calculation of the mean of vectors, and the selection of the nearest centers.

### Multi-threading

The types described here are in the package `dog.giraffe.thread`.

An `Executor` can take a `Block` of code and will eventually run it.
For most use-cases this is too low-level and is used to build higher level abstractions.

A `Continuation` is a piece of code that can process a result.
In a way a `Continuation` waits for some result to be produced.
A continuation expects two kind of outcome for any computation.
The computation may be successful, and calls `Continuation.completed()` with the result.
A computation may result in an exception, and signals this by calling `Continuation.failed()`.
A computations which never calls back the continuation is considered non-terminating.

The class `Continuations` have helper methods for high-level manipulation of continuations.
`Continuations.map()` composes an `AsyncFunction` and a `Continuation`.
This way computations can be built backward from the final continuation.
An `AsyncFunction` is the asynchronous variant of a function, it maps an input value to an output value,
and completes a continuation with the output value.

An `AsyncSupplier` can produce a value without any further input, and completes a continuation.

`Continuations.forkJoin()` is the high-level multi-threaded operation.
It executes in parallel a list of `AsyncSupplier`, the forks, and gathers all the results.
When all suppliers become completed, it will complete the join continuation.
If all fork was successful, it will complete the join with the list of all the result,
in the same order as in the forks list.
If any of the forks failed, it will fail the join too.
The join continuation don't need to do any further synchronization,
the completion of all the forks happens-before the completion of the join.

### Clustering algorithms

Classes describing the clustering algorithm are in the package `dog.giraffe.cluster`.

The result of a clustering is `Clusters`. This contains the cluster centers and the error of the clustering.
Error need to be comparable between two clustering if their parameters differ only in the number of clusters.

A cluster center may contain more than one vector.
The distance of a data point to a cluster is defined to be the minimum of the distances to the vectors in that cluster.
Otsu's method produces clusters with more than one center as threshold selection ignores the euclidean distance.

`ClusteringStrategy` captures all the peculiarities of a clustering algorithm,
it needs only the data points to complete a clustering.

`ClusteringStrategy`s can be composed.
`ClusteringStrategy.best()` runs all the provided strategies and select the result with the smallest error.
`ClusteringStrategy.elbow()` implements the elbow method already described.

`Isodata.isodata()`, `KMeans.kMeans()`, `Otsu.circular()`, and `Otsu.linear()` creates the strategies
described above.

`InitialCenters` are the implementations of algorithms for the selection of initial cluster centers.
`ReplaceEmptyCluster` replaces a cluster when it becomes empty.
These are used by ISODATA and k-means.

### Image manipulation

The classes describing images are in `dog.giraffe.image` and `dog.giraffe.image.transform`.

The main abstract property of an `Image` is the ability to be read line-by-line by multiple threads simultaneously.
Also images can depend on other images, and require preparation to start reading the line.

The lifecycle of a directed acyclic graph of images is:
- create the image DAG
- prepare all the images in a topological order of the DAG
- read all the lines of the terminal images and produce a result.

`Image.prepare()` is an asynchronous operation, and it can take a lots of times.
Clustering transformations creates the clusters while preparing themselves.
During prepare an image can read the lines of all their dependencies as they are already prepared.
`PrepareImages.prepareImages()` is a helper method to correctly prepare images.

The lines of an image can only be read indirectly through an `Image.Reader` object,
which can be created in any number by the `Image.reader()` method.
These reader objects capture all the thread-unsafe aspects required to read a line.
This way multiple threads using their own readers can safely read lines simultaneously.

An `ImageReader` is an `Image` that depends on no other images.
A `FileImageReader` reads the lines of an image directly from disk.
A `BufferedImageReader` stores an image in a memory buffer and servers read request from its internal buffer.
A `BufferedImageReader` can be created from a file on disk and from a `java.awt.image.BufferedImage` instance.

The package `dog.giraffe.image.transform` contains the transformations described above.

An `ImageWriter` can be used to generate a final image.
An `ImageWriter` can write an image line-by-line, matching the behaviour of an `ImageReader`.
The `ImageWriter.Line` can be used to write one line of the image,
and these multiple threads can write line simultaneously through their own `ImageWriter.Line` objects.
A `FileImageWriter` directly writes every line to disk.
A `BufferedImageWriter` writes all lines to an internal memory buffer
and can be asked to write this buffer to disk or to create a `java.awt.image.BufferedImage` instance.

### API documentation

https://zooflavor.github.io/ttaf/javadoc/index.html

## GUI tool

## References
<a id="1">[1]</a>
Segmentation Using Saturation Thresholding and Its Application in Content-Based Retrieval of Images
<br>
A. Vadivel, 2004

<a id="2">[2]</a>
ISODATA, A NOVEL METHOD OF DATA ANALYSIS AND PATTERN CLASSIFICATION
<br>
G. Ball, D. J. Hall, 1965

<a id="3">[3]</a>
A Threshold Selection Method from Gray-Level Histograms
<br>
Nobuyuki Otsu, 1979

<a id="4">[4]</a>
Circular histogram thresholding for color image segmentation
<br>
D.-C. Tseng, Y.-F. Li, and C.-T. Tung, 1995

<a id="5">[5]</a>
Transformation of a high-dimensional color space for material classification
<br>
Huajian Liu, Sang-Heon Lee, Javaan Singh Chahl, 2017
