
benchmark=/home/guanpu/Desktop/DFix-Public/public/DFix/benchmark
toy=$benchmark/toy
zk1270=$benchmark/zk-1270
zk1144=$benchmark/zk-1144
ca1011=$benchmark/ca-1011
mr4637=$benchmark/mr-4637
mr3274=$benchmark/mr-3274
walalib=../../lib/WALA
classpath=$walalib/com.ibm.wala.core/target/classes:$walalib/com.ibm.wala.util/target/classes:$walalib/com.ibm.wala.shrike/target/classes:$walalib/com.ibm.wala.core.testdata/target/classes:$walalib/com.ibm.wala.core.tests/target/classes:.
#classpath=$walalib/com.ibm.wala.core/bin:$walalib/com.ibm.wala.util/bin:$walalib/com.ibm.wala.shrike/bin:$walalib/com.ibm.wala.core.testdata/bin:$walalib/com.ibm.wala.ide/bin:$walalib/com.ibm.wala.core.tests/bin:
#echo $toy

#time java -Xmx10G -cp $classpath uchicago.dfix.ssat $ca1011
#time java -Xmx10G -cp $classpath uchicago.dfix.ssat $mr4637
time java -Xmx10G -cp $classpath uchicago.dfix.ssat $mr3274
#time java -Xmx2G -cp $classpath uchicago.dfix.ssat $zk1144
#time java -Xmx2G -cp $classpath uchicago.dfix.ssat $zk1270
#time java -Xmx2G -cp $classpath uchicago.dfix.ssat $toy

