#!/bin/bash

set -e
set -x

rm -rf testimages/out
mkdir -p testimages/out/misc
ls -1 testimages/misc/* > testimages/out/misc/list

for algo in k-means isodata
do
./giraffe-cmd.sh -i testimages/P.t.altaica_Tomak_Male.jpg -o testimages/out/macska-rgb-$algo-elbow.jpg --log testimages/out/macska-rgb-$algo-elbow.jpg.log -a $algo -e --min 2 --max 10 --max-iterations 1000
./giraffe-cmd.sh -i testimages/P.t.altaica_Tomak_Male.jpg -o testimages/out/macska-hue-$algo-elbow.jpg --log testimages/out/macska-hue-$algo-elbow.jpg.log -a $algo -e --min 2 --max 10 --max-iterations 1000 --image-transform hue
./giraffe-cmd.sh -i testimages/P.t.altaica_Tomak_Male.jpg -o testimages/out/macska-hyper-hue-$algo-elbow.jpg --log testimages/out/macska-hyper-hue-$algo-elbow.jpg.log -a $algo -e --min 2 --max 10 --max-iterations 1000 --image-transform hyper-hue
./giraffe-cmd.sh -i testimages/P.t.altaica_Tomak_Male.jpg -o testimages/out/macska-normalized-hyper-hue-$algo-elbow.jpg --log testimages/out/macska-normalized-hyper-hue-$algo-elbow.jpg.log -a $algo -e --min 2 --max 10 --max-iterations 1000 --image-transform "normalized-hyper-hue(0.05)"
./giraffe-cmd.sh -i testimages/P.t.altaica_Tomak_Male.jpg -o testimages/out/macska-saturation-based-hue-$algo-elbow.jpg -a $algo -e --min 2 --max 10 --max-iterations 1000 --saturation-based hue
./giraffe-cmd.sh -i testimages/P.t.altaica_Tomak_Male.jpg -o testimages/out/macska-saturation-based-hyper-hue-$algo-elbow.jpg -a $algo -e --min 2 --max 10 --max-iterations 1000 --saturation-based hyper-hue
./giraffe-cmd.sh -i testimages/huetest1.tiff -o testimages/out/huetest1-$algo.jpg -a $algo -e --min 2 --max 10 --image-transform hue
./giraffe-cmd.sh -i testimages/huetest2.tiff -o testimages/out/huetest2-$algo.jpg -a $algo -e --min 2 --max 10 --image-transform hue

./giraffe-cmd.sh --batch-mode -i testimages/out/misc/list -o "testimages/out/misc/$algo-\$FILE\$EXT" --log "testimages/out/misc/$algo-\$FILE\$EXT.log" -a $algo -e --min 2 --max 10 --max-iterations 1000 --batch-parallel-images 2
done

./giraffe-cmd.sh -i testimages/huetest1.tiff -o testimages/out/huetest1-otsu-circular.jpg -a otsu-circular -e=false --min 2 --max 2 --bins 10 --image-transform hue
./giraffe-cmd.sh -i testimages/huetest1.tiff -o testimages/out/huetest1-otsu-linear.jpg -a otsu -e=false --min 2 --max 2 --bins 1000 --image-transform hue
./giraffe-cmd.sh -i testimages/huetest2.tiff -o testimages/out/huetest2-otsu-circular.jpg -a otsu-circular -e=false --min 2 --max 2 --bins 10 --image-transform hue
./giraffe-cmd.sh -i testimages/huetest2.tiff -o testimages/out/huetest2-otsu-linear.jpg -a otsu -e=false --min 2 --max 2 --bins 10 --image-transform hue

found=0
for algo in k-means isodata
do
for satimg in LC08_L1TP_188027_20200420_20200508_01_T1.tif ../ttaf2/LC08_L1TP_188027_20200420_20200508_01_T1.tif
do
if [[ -f $satimg ]]; then
found=1
./giraffe-cmd.sh -i $satimg -o testimages/out/valami-rgb-normalized-$algo.jpg -a $algo -e --min 2 --max 10 --image-transform "select(1,2,3)" --image-transform "normalize-deviation(3.0)" --mask 1851,4,14,6256,14,6256,6119,8066,6119,8066,7964,1786,7964,1786,1851,4
./giraffe-cmd.sh -i $satimg -o testimages/out/valami-rgb-hyper-hue-normalized-$algo.jpg -a $algo -e --min 2 --max 10 --image-transform "select(1,2,3)" --image-transform "normalize-deviation(3.0)" --image-transform hyper-hue --mask 1851,4,14,6256,14,6256,6119,8066,6119,8066,7964,1786,7964,1786,1851,4
./giraffe-cmd.sh -i $satimg -o testimages/out/valami-all-hyper-hue-normalized-$algo.jpg -a $algo -e --min 2 --max 10 --image-transform "normalize-deviation(3.0)" --image-transform hyper-hue --mask 1851,4,14,6256,14,6256,6119,8066,6119,8066,7964,1786,7964,1786,1851,4
./giraffe-cmd.sh -i $satimg -o testimages/out/valami-nir-red-$algo.jpg -a $algo -e --min 2 --max 10 --image-transform "select(3,4)" --mask 1851,4,14,6256,14,6256,6119,8066,6119,8066,7964,1786,7964,1786,1851,4
./giraffe-cmd.sh -i $satimg -o testimages/out/valami-ndvi-$algo.jpg -a $algo -e --min 2 --max 10 --image-transform "select(3,4)" --image-transform normalized-difference-vegetation-index --mask 1851,4,14,6256,14,6256,6119,8066,6119,8066,7964,1786,7964,1786,1851,4
./giraffe-cmd.sh -i $satimg -o testimages/out/valami-nir-red-normalized-hyper-hue-$algo.jpg -a $algo -e --min 2 --max 3 --image-transform "select(3,4)" --image-transform "normalized-hyper-hue(0.01)" --mask 1851,4,14,6256,14,6256,6119,8066,6119,8066,7964,1786,7964,1786,1851,4
fi
done
done

if [ "$found" -eq 0 ] ; then
 echo "Cannot find sat image, skipping"
fi
