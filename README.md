# classifyCloud
Setup:
1. Set AWS access id and secret key environment variables on local
machine.
2. Create deep learning EC2 instance with ubuntu operating system
and set .aws/credentials and .aws/config files. Below is the config file.
3. Create a directory named proj1-worker in home directory.
4. Copy classify_image.py, worker.sh, worker.py to the directory created from the above step.
5. Copy the worker.service file to /etc/systemd/system directory and restart the systemd service.
6. Register the AMI for the above instance and replace ami-id in the application.properties file with the created ami id.
7. Create 3 Standard queues named as request, response and terminate queue.
8. Create a S3 bucket named ccimagerecognition.
Note: All the above AWS services should be created in us-west-1 region.
Steps to run:
1. Start the Apache tomcat server using the below command.
mvn spring-boot:run
2. Open browser.
3. Make sure that tomcat server is running by entering the following
url.
  http://localhost:50003
 
4. Provide a image url by making GET request to the server as shown below.
http://localhost:50003​/cloudimagerecognition.php?input=<url of the image>
5. You should be able to see a key value pair as the output where key is image name and the value is the class of the image. It should take about a minute for the result to show up.
