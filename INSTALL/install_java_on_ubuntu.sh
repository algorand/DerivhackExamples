#Install Java
sudo apt-get update
sudo apt-get install default-jdk


#Modify bashrc to add the new java home                                                                                                                                
echo 'JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/' >> ~/.bashrc
echo 'export JAVA_HOME' >> ~/.bashrc
echo 'PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
echo 'export PATH=/usr/local/bin:$PATH' >>~/.bashrc
