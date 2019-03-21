
benchmark=/home/guanpu/Desktop/DFix-Public/public/DFix/benchmark
toy=$benchmark/toy
zk1270=$benchmark/zk-1270
zk1144=$benchmark/zk-1144
ca1011=$benchmark/ca-1011
mr4637=$benchmark/mr-4637
mr3274=$benchmark/mr-3274
walalib=../../lib/WALA
classpath=$walalib/com.ibm.wala.core/target/classes:$walalib/com.ibm.wala.util/target/classes:$walalib/com.ibm.wala.shrike/target/classes:$walalib/com.ibm.wala.core.testdata/target/classes:$walalib/com.ibm.wala.core.tests/target/classes:.
#walalib=/mnt/storage/packages/wala/WALA-R_1.3.5
#classpath=$walalib/com.ibm.wala.core/bin:$walalib/com.ibm.wala.util/bin:$walalib/com.ibm.wala.shrike/bin:$walalib/com.ibm.wala.core.testdata/bin:$walalib/com.ibm.wala.core.tests/bin:/home/cstjygpl/DFix/lib/guava-19.0.jar:.
#classpath=$walalib/com.ibm.wala.core/bin:$walalib/com.ibm.wala.util/bin:$walalib/com.ibm.wala.shrike/bin:$walalib/com.ibm.wala.core.testdata/bin:$walalib/com.ibm.wala.ide/bin:$walalib/com.ibm.wala.core.tests/bin:
app=uchicago.dfix.prover
#time java -Xmx10G -cp $classpath $app $ca1011 V
time java -Xmx10G -cp $classpath $app $mr4637 A
time java -Xmx10G -cp $classpath $app $mr3274 A
time java -Xmx2G -cp $classpath $app $zk1144 A
time java -Xmx2G -cp $classpath $app $zk1270 A
#time java -Xmx2G -cp $classpath $app $toy A

