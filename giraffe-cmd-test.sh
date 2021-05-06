#!/bin/bash

mkdir -p ../ttaf2/testout
rm -rf ../ttaf2/testout/*
./giraffe-cmd.sh -i P.t.altaica_Tomak_Male.jpg -o ../ttaf2/testout/macska-rgb-kmeans-elbow.tiff --log ../ttaf2/testout/macska-rgb-kmeans-elbow.tiff.log -a k-means -e --min 2 --max 10 --max-iterations 1000
./giraffe-cmd.sh -i P.t.altaica_Tomak_Male.jpg -o ../ttaf2/testout/macska-hue-kmeans-elbow.tiff --log ../ttaf2/testout/macska-hue-kmeans-elbow.tiff.log -a k-means -e --min 2 --max 10 --max-iterations 1000 --image-transform hue
./giraffe-cmd.sh -i P.t.altaica_Tomak_Male.jpg -o ../ttaf2/testout/macska-hyper-hue-kmeans-elbow.tiff --log ../ttaf2/testout/macska-hyper-hue-kmeans-elbow.tiff.log -a k-means -e --min 2 --max 10 --max-iterations 1000 --image-transform hyper-hue
./giraffe-cmd.sh -i P.t.altaica_Tomak_Male.jpg -o ../ttaf2/testout/macska-normalized-hyper-hue-kmeans-elbow.tiff --log ../ttaf2/testout/macska-normalized-hyper-hue-kmeans-elbow.tiff.log -a k-means -e --min 2 --max 10 --max-iterations 1000 --image-transform "normalized-hyper-hue(0.05)"
./giraffe-cmd.sh -i ../ttaf2/huetest1.tiff -o ../ttaf2/testout/huetest1-otsu-circular.tiff -a otsu-circular -e=false --min 2 --max 2 --bins 10 --image-transform hue
./giraffe-cmd.sh -i ../ttaf2/huetest1.tiff -o ../ttaf2/testout/huetest1-otsu-linear.tiff -a otsu -e=false --min 2 --max 2 --bins 1000 --image-transform hue
./giraffe-cmd.sh -i ../ttaf2/huetest2.tiff -o ../ttaf2/testout/huetest2-otsu-circular.tiff -a otsu-circular -e=false --min 2 --max 2 --bins 10 --image-transform hue
./giraffe-cmd.sh -i ../ttaf2/huetest2.tiff -o ../ttaf2/testout/huetest2-otsu-linear.tiff -a otsu -e=false --min 2 --max 2 --bins 10 --image-transform hue
./giraffe-cmd.sh -i ../ttaf2/LC08_L1TP_188027_20200420_20200508_01_T1.tif -o ../ttaf2/testout/valami-rgb-normalized-kmeans.tiff -a k-means -e --min 2 --max 10 --image-transform "select(1,2,3)" --image-transform "normalize-variance(3.0)" --mask 1851,4,14,6256,14,6256,6119,8066,6119,8066,7964,1786,7964,1786,1851,4
./giraffe-cmd.sh -i ../ttaf2/LC08_L1TP_188027_20200420_20200508_01_T1.tif -o ../ttaf2/testout/valami-rgb-hyper-hue-normalized-kmeans.tiff -a k-means -e --min 2 --max 10 --image-transform "select(1,2,3)" --image-transform "normalize-variance(3.0)" --image-transform hyper-hue --mask 1851,4,14,6256,14,6256,6119,8066,6119,8066,7964,1786,7964,1786,1851,4
./giraffe-cmd.sh -i ../ttaf2/LC08_L1TP_188027_20200420_20200508_01_T1.tif -o ../ttaf2/testout/valami-all-hyper-hue-normalized-kmeans.tiff -a k-means -e --min 2 --max 10 --image-transform "normalize-variance(3.0)" --image-transform hyper-hue --mask 1851,4,14,6256,14,6256,6119,8066,6119,8066,7964,1786,7964,1786,1851,4
./giraffe-cmd.sh -i ../ttaf2/LC08_L1TP_188027_20200420_20200508_01_T1.tif -o ../ttaf2/testout/valami-nir-red-kmeans.tiff -a k-means -e --min 2 --max 10 --image-transform "select(3,4)" --mask 1851,4,14,6256,14,6256,6119,8066,6119,8066,7964,1786,7964,1786,1851,4
./giraffe-cmd.sh -i ../ttaf2/LC08_L1TP_188027_20200420_20200508_01_T1.tif -o ../ttaf2/testout/valami-ndvi-kmeans.tiff -a k-means -e --min 2 --max 10 --image-transform "select(3,4)" --image-transform normalized-difference-vegetation-index --mask 1851,4,14,6256,14,6256,6119,8066,6119,8066,7964,1786,7964,1786,1851,4
./giraffe-cmd.sh -i ../ttaf2/LC08_L1TP_188027_20200420_20200508_01_T1.tif -o ../ttaf2/testout/valami-nir-red-normalized-hyper-hue-kmeans.tiff -a k-means -e --min 2 --max 3 --image-transform "select(3,4)" --image-transform "normalized-hyper-hue(0.01)" --mask 1851,4,14,6256,14,6256,6119,8066,6119,8066,7964,1786,7964,1786,1851,4
./giraffe-cmd.sh -i P.t.altaica_Tomak_Male.jpg -o ../ttaf2/testout/macska-saturation-based-hue-kmeans-elbow.tiff -a k-means -e --min 2 --max 10 --max-iterations 1000 --saturation-based hue
./giraffe-cmd.sh -i P.t.altaica_Tomak_Male.jpg -o ../ttaf2/testout/macska-saturation-based-hyper-hue-kmeans-elbow.tiff -a k-means -e --min 2 --max 10 --max-iterations 1000 --saturation-based hyper-hue

mkdir ../ttaf2/testout/misc
ls -1 ../ttaf2/a/misc/* > ../ttaf2/testout/misc/list
./giraffe-cmd.sh --batch-mode -i ../ttaf2/testout/misc/list -o '../ttaf2/testout/misc/$FILE$EXT' --log '../ttaf2/testout/misc/$FILE$EXT.log' -a k-means -e --min 2 --max 10 --max-iterations 1000 --batch-parallel-images 2
