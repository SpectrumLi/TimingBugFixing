benchmark=/home/guanpu/Desktop/DFix-Public/public/DFix/benchmark
toy=$benchmark/toy
zk1270=$benchmark/zk-1270
zk1144=$benchmark/zk-1144
ca1011=$benchmark/ca-1011
mr4637=$benchmark/mr-4637
mr3274=$benchmark/mr-3274
hb4539=$benchmark/hb-4539
hb4729=$benchmark/hb-4729

walalib=../../lib/WALA
classpath=$walalib/com.ibm.wala.core/target/classes:$walalib/com.ibm.wala.util/target/classes:$walalib/com.ibm.wala.shrike/target/classes:$walalib/com.ibm.wala.core.testdata/target/classes:$walalib/com.ibm.wala.core.tests/target/classes:$walalib/../guava-19.0.jar:$walalib/../javaparser-core-3.2.4-SNAPSHOT.jar:$walalib/../javaparser-core-generators-3.2.4-SNAPSHOT.jar:$walalib/../javaparser-metamodel-generator-3.2.4-SNAPSHOT.jar:.

app=uchicago.dfix.finalpatch
time java -Xmx10G -cp $classpath $app $ca1011 
#time java -Xmx10G -cp $classpath $app $ca1011 > result/p-ca1011
java -Xmx10G -cp $classpath $app $mr4637 
java -Xmx10G -cp $classpath $app $mr3274
time java -Xmx2G -cp $classpath $app $hb4539 
time java -Xmx2G -cp $classpath $app $hb4729  
#time java -Xmx2G -cp $classpath $app $hb4539  > result/p-hb4539
#time java -Xmx2G -cp $classpath $app $hb4729  > result/p-hb4729
#time java -Xmx10G -cp $classpath $app $mr3274 > result/p-mr3274
#time java -Xmx10G -cp $classpath $app $mr4637 > result/p-mr4637 

#java -Xmx2G -cp $classpath $app $zk1144 > result/p-zk1144
#java -Xmx2G -cp $classpath $app $zk1270 > result/p-zk1270 
time java -Xmx2G -cp $classpath $app $zk1270 
time java -Xmx2G -cp $classpath $app $zk1144

#java -Xmx2G -cp $classpath $app $toy 

