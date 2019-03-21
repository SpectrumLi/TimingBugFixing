benchmark=/home/guanpu/Desktop/DFix-Public/public/DFix/benchmark
toy=$benchmark/toy
zk1270=$benchmark/zk-1270
zk1144=$benchmark/zk-1144
ca1011=$benchmark/ca-1011
mr4637=$benchmark/mr-4637
mr3274=$benchmark/mr-3274
walalib=../../lib/WALA
classpath=$walalib/com.ibm.wala.core/target/classes:$walalib/com.ibm.wala.util/target/classes:$walalib/com.ibm.wala.shrike/target/classes:$walalib/com.ibm.wala.core.testdata/target/classes:$walalib/com.ibm.wala.core.tests/target/classes:$walalib/../guava-19.0.jar:.
app=uchicago.dfix.sedetector
#time java -Xmx10G -cp $classpath $app $ca1011 > se-ca1011
time java -Xmx10G -cp $classpath $app $mr4637 > result/se-mr4637
#java -Xmx10G -cp $classpath $app $mr4637 
time java -Xmx10G -cp $classpath $app $mr3274 > result/se-mr3274
#java -Xmx10G -cp $classpath $app $mr3274
#java -Xmx2G -cp $classpath $app $zk1144 
time java -Xmx2G -cp $classpath $app $zk1144 > result/se-zk1144
#java -Xmx2G -cp $classpath $app $zk1270 
time java -Xmx2G -cp $classpath $app $zk1270 > result/se-zk1270
#java -Xmx2G -cp $classpath $app $toy 

