#!/bin/bash
# Install Amazon Corretto 22 on Ubuntu/Debian

set -e

echo "Installing Amazon Corretto 22..."

# 1. Install prerequisites
echo "Step 1: Installing prerequisites..."
sudo apt-get update
sudo apt-get install -y wget gpg

# 2. Download and install Amazon GPG key
echo "Step 2: Adding Amazon GPG key..."
wget -O- https://apt.corretto.aws/corretto.key | sudo gpg --dearmor -o /usr/share/keyrings/corretto.keyring

# 3. Add Amazon Corretto repository
echo "Step 3: Adding Amazon Corretto repository..."
echo 'deb [signed-by=/usr/share/keyrings/corretto.keyring] https://apt.corretto.aws stable main' | sudo tee /etc/apt/sources.list.d/corretto.list

# 4. Update package list and install Corretto 22
echo "Step 4: Installing Amazon Corretto 22..."
sudo apt-get update
sudo apt-get install -y java-22-amazon-corretto-jdk

# 5. Verify installation
echo ""
echo "Installation complete! Verifying..."
echo ""
java -version
echo ""
javac -version
echo ""
echo "Amazon Corretto 22 is now installed!"

