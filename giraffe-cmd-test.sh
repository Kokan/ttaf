#!/bin/bash

set -e
set -x

rm -rf testimages/out
mkdir -p testimages/out/misc
ls -1 testimages/misc/* > testimages/out/misc/list

./giraffe-cmd.sh -i testimages/P.t.altaica_Tomak_Male.jpg -o testimages/out/macska-rgb-kmeans-elbow.tiff --log testimages/out/macska-rgb-kmeans-elbow.tiff.log -a k-means -e --min 2 --max 10 --max-iterations 1000
./giraffe-cmd.sh -i testimages/P.t.altaica_Tomak_Male.jpg -o testimages/out/macska-hue-kmeans-elbow.tiff --log testimages/out/macska-hue-kmeans-elbow.tiff.log -a k-means -e --min 2 --max 10 --max-iterations 1000 --image-transform hue
./giraffe-cmd.sh -i testimages/P.t.altaica_Tomak_Male.jpg -o testimages/out/macska-hyper-hue-kmeans-elbow.tiff --log testimages/out/macska-hyper-hue-kmeans-elbow.tiff.log -a k-means -e --min 2 --max 10 --max-iterations 1000 --image-transform hyper-hue
./giraffe-cmd.sh -i testimages/P.t.altaica_Tomak_Male.jpg -o testimages/out/macska-normalized-hyper-hue-kmeans-elbow.tiff --log testimages/out/macska-normalized-hyper-hue-kmeans-elbow.tiff.log -a k-means -e --min 2 --max 10 --max-iterations 1000 --image-transform "normalized-hyper-hue(0.05)"
./giraffe-cmd.sh -i testimages/P.t.altaica_Tomak_Male.jpg -o testimages/out/macska-saturation-based-hue-kmeans-elbow.tiff -a k-means -e --min 2 --max 10 --max-iterations 1000 --saturation-based hue
./giraffe-cmd.sh -i testimages/P.t.altaica_Tomak_Male.jpg -o testimages/out/macska-saturation-based-hyper-hue-kmeans-elbow.tiff -a k-means -e --min 2 --max 10 --max-iterations 1000 --saturation-based hyper-hue
./giraffe-cmd.sh -i testimages/huetest1.tiff -o testimages/out/huetest1-otsu-circular.tiff -a otsu-circular -e=false --min 2 --max 2 --bins 10 --image-transform hue
./giraffe-cmd.sh -i testimages/huetest1.tiff -o testimages/out/huetest1-otsu-linear.tiff -a otsu -e=false --min 2 --max 2 --bins 1000 --image-transform hue
./giraffe-cmd.sh -i testimages/huetest2.tiff -o testimages/out/huetest2-otsu-circular.tiff -a otsu-circular -e=false --min 2 --max 2 --bins 10 --image-transform hue
./giraffe-cmd.sh -i testimages/huetest2.tiff -o testimages/out/huetest2-otsu-linear.tiff -a otsu -e=false --min 2 --max 2 --bins 10 --image-transform hue

./giraffe-cmd.sh --batch-mode -i testimages/out/misc/list -o 'testimages/out/misc/$FILE$EXT' --log 'testimages/out/misc/$FILE$EXT.log' -a k-means -e --min 2 --max 10 --max-iterations 1000 --batch-parallel-images 2

mask="--mask 1851,4,14,6256,14,6256,6119,8066,6119,8066,7964,1786,7964,1786,1851,4"
found=0
for satimg in LC08_L1TP_188027_20200420_20200508_01_T1.tif ../ttaf2/LC08_L1TP_188027_20200420_20200508_01_T1.tif
do
if [[ -f $satimg ]]; then
found=1
./giraffe-cmd.sh -i $satimg -o testimages/out/valami-rgb-normalized-kmeans.tiff -a k-means -e --min 2 --max 10 --image-transform "select(1,2,3)" --image-transform "normalize-variance(3.0)" "$mask"
./giraffe-cmd.sh -i $satimg -o testimages/out/valami-rgb-hyper-hue-normalized-kmeans.tiff -a k-means -e --min 2 --max 10 --image-transform "select(1,2,3)" --image-transform "normalize-variance(3.0)" --image-transform hyper-hue "$mask"
./giraffe-cmd.sh -i $satimg -o testimages/out/valami-all-hyper-hue-normalized-kmeans.tiff -a k-means -e --min 2 --max 10 --image-transform "normalize-variance(3.0)" --image-transform hyper-hue "$mask"
./giraffe-cmd.sh -i $satimg -o testimages/out/valami-nir-red-kmeans.tiff -a k-means -e --min 2 --max 10 --image-transform "select(3,4)" "$mask"
./giraffe-cmd.sh -i $satimg -o testimages/out/valami-ndvi-kmeans.tiff -a k-means -e --min 2 --max 10 --image-transform "select(3,4)" --image-transform normalized-difference-vegetation-index "$mask"
./giraffe-cmd.sh -i $satimg -o testimages/out/valami-nir-red-normalized-hyper-hue-kmeans.tiff -a k-means -e --min 2 --max 3 --image-transform "select(3,4)" --image-transform "normalized-hyper-hue(0.01)" "$mask"
fi
done

if [ "$found" -eq 0 ] ; then
 echo "Cannot find sat image, skipping"
fi
