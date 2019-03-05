
benchmark=/home/cstjygpl/DFix/benchmark
toy=$benchmark/toy
zk1270=$benchmark/zk-1270
zk1144=$benchmark/zk-1144
ca1011=$benchmark/ca-1011
mr4637=$benchmark/mr-4637
mr3274=$benchmark/mr-3274
walalib=/mnt/storage/packages/wala/WALA-R_1.3.5
locallib=/home/cstjygpl/DFix/lib
classpath=$walalib/com.ibm.wala.core/bin:$walalib/com.ibm.wala.util/bin:$walalib/com.ibm.wala.shrike/bin:$walalib/com.ibm.wala.core.testdata/bin:$walalib/com.ibm.wala.core.tests/bin:$locallib/javaparser-core-3.2.4-SNAPSHOT.jar:$locallib/javaparser-core-generators-3.2.4-SNAPSHOT.jar:$locallib/javaparser-metamodel-generator-3.2.4-SNAPSHOT.jar:.
#echo $toy
#app=uchicago.dfix.ssat
app=uchicago.dfix.fclone
#java -Xmx10G -cp $classpath $app $ca1011 V
#java -Xmx10G -cp $classpath $app $mr4637 
#java -Xmx10G -cp $classpath $app $mr3274 
#java -Xmx2G -cp $classpath $app $zk1144 
#java -Xmx2G -cp $classpath $app $zk1270 
java -Xmx2G -cp $classpath $app /home/cstjygpl/DFix/software/toy/foo.java

