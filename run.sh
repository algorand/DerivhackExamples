##Example Java scripts to use Algorand with the ISDA CDM

##Commit the execution file to the blockchain
mvn -s settings.xml exec:java -Dexec.mainClass="com.algorand.demo.CommitEvent" \
 -Dexec.args="./Files/UC1_block_execute_BT1.json" -e -q

## Commit the allocation file to the blockchain
 mvn -s settings.xml exec:java -Dexec.mainClass="com.algorand.demo.CommitEvent" \
 -Dexec.args="./Files/UC2_allocation_execution_AT1.json" -e -q

## Create Affirmations from the Allocation file and Commit Them
mvn -s settings.xml exec:java -Dexec.mainClass="com.algorand.demo.AffirmationStep"\
 -Dexec.args="./Files/UC2_allocation_execution_AT1.json"   -e  -q 


