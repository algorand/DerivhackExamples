##Install the latest version of Java using homebrew
brew tap caskroom/cask
brew cask info java  
brew cask install java 

#Modify bash_profile to add the new java home
echo 'JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-13.jdk/Contents/Home/' >> ~/.bash_profile
echo 'export JAVA_HOME' >> ~/.bash_profile
echo 'PATH=$JAVA_HOME/bin:$PATH' >> ~/.bash_profile
echo 'export PATH=/usr/local/bin:$PATH' >>~/.bash_profile

