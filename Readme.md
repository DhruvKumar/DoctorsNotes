<h2>Summary</h2>
<p>
Because patients visit many doctors, trends in their ailments and complaints may be difficult to identify.
The steps in this article will help you address exactly this problem by creating a TagCloud of the most frequent complaints per patient.  Below is a sample:
</p>
<p>
	<img src="/storage/attachments/2464-full-banana-screenshot.png" alt="Banana Dashboard screenshot">
</p>
<p>
	We will generate random HL7 MDM^T02 (v2.3) messages that contain a doctor's note about a fake patient and that patient's fake complaint to their doctor. Apache NiFi will be used to parse these messages and send them to Apache Solr. Finally Banana is used to create the visual dashboard.
</p>
<p>
	In the middle of the dashboard is a TagCloud where the more frequently mentioned symptoms for a selected patient appear larger than others.  Because this project relies on randomly generated data, some interesting results are possible.  In this case, I got lucky and all the symptoms seem related to the patient's most frequent complaint: Morning Alcohol Drinking. The list of all possible symptoms comes from Google searches.
</p>
<p>
	<img src="/storage/attachments/2483-frequent-symptoms-screenshot.png">
</p>
<h2>
Summary of steps
</h2>
<ol>
	<li>Download and install the HDP Sandbox </li>
	<li>Download and install the latest NiFi release </li>
	<li>Download the HL7 message generator</li>
	<li>Create a Solr dashboard to visualize the results</li>
	<li>Create and execute a new NiFi flow</li>
</ol>
<h2>Detailed Step-by-step guide </h2>
<h3>1. Download and install the HDP Sandbox</h3>
<p>
	Download the latest (2.3 as of this writing) HDP Sandbox 
	<a href="http://hortonworks.com/products/hortonworks-sandbox/#install.">here</a>. Import it into VMware or VirtualBox, start the instance, and update the DNS entry on your host machine to point to the new instance’s IP. On Mac, edit
	<em>/etc/hosts</em>, on Windows, edit <em>%systemroot%\system32\drivers\etc\</em> as administrator and add a line similar to the below:
</p>
<pre>
192.168.56.102  sandbox sandbox.hortonworks.com
</pre>
<h3>2. Download and install the latest NiFi release</h3>
<p>
	Follow the directions 
	<a href="https://nifi.apache.org/docs.html">here</a>.  These were the steps that I executed for 0.5.1
</p>
<pre>
wget http://apache.cs.utah.edu/nifi/0.5.1/nifi-0.5.1-bin.zip -O /tmp/nifi-0.5.1-bin.zip
cd /opt/
unzip  /tmp/nifi-0.5.1-bin.zip
useradd nifi
chown -R nifi:nifi /opt/nifi-0.5.1/
perl -pe 's/run.as=.*/run.as=nifi/' -i /opt/nifi-0.5.1/conf/bootstrap.conf
perl -pe 's/nifi.web.http.port=8080/nifi.web.http.port=9090/' -i /opt/nifi-0.5.1/conf/nifi.properties
/opt/nifi-0.5.1/bin/nifi.sh start
</pre>
<h3>3. Download the HL7 message generator</h3>
<p>
	A big thank you to 
	<a href="http://hl7api.sourceforge.net/">HAPI</a> for their excellent library to parse and create HL7 messages on which my code relies. The generator creates a very simple MDM^T02 that includes an in-line note from a doctor.  MDM stands for Medical Document Management, and T02 specifies that this is a message
for a new document.  For more details about this message type read 
	<a href="http://corepointhealth.com/resource-center/hl7-resources/hl7-mdm-message">this</a> document.  Here is a sample message for Beatrice Cunningham:
</p>
<pre>
MSH|^~\&|||||20160229002413.415-0500||MDM^T02|7|P|2.3
EVN|T02|201602290024
PID|1||599992601||cunningham^beatrice^||19290611|F
PV1|1|O|Burn center^60^71
TXA|1|CN|TX|20150211002413||||||||DOC-ID-10001|||||AU||AV
OBX|1|TX|1001^Reason For Visit: |1|Evaluated patient for skin_scaling. ||||||F
</pre>
<p>
	As a pre-requisite to executing the code, we need to install Java 8.  Execute this on the Sandbox:
</p>
<pre>
yum -y install java-1.8.0-openjdk.x86_64
</pre>
<p>
	Now, download the pre-build jar file that has the HL7 generator and execute it to create a single message in
	<em>/tmp/hl7-messages.</em> I chose to store the jar file in <em>/var/ftp/pub</em> because my IDE uploads files during code development.  If you change this directory, also change it in the NiFi flow.
</p>
<pre>
mkdir -p /var/ftp/pub
cd /var/ftp/pub
wget https://raw.githubusercontent.com/vzlatkin/DoctorsNotes/master/target/hl7-generator-1.0-SNAPSHOT-shaded.jar
mkdir -p /tmp/hl7-messages/
/usr/lib/jvm/jre-1.8.0/bin/java -cp hl7-generator-1.0-SNAPSHOT-shaded.jar  com.hortonworks.example.Main 1 /tmp/hl7-messages
chown -R nifi:nifi /tmp/hl7-messages/
</pre>
<h3>4. Create a Solr dashboard to visualize the results</h3>
<p>
	Now we need to configure Solr to ignore some words that don't add value.  We do this by modifying 
	<em>stopwords.txt</em>
</p>
<pre>
cat  &lt;&lt;EOF &gt; /opt/lucidworks-hdpsearch/solr/server/solr/configsets/data_driven_schema_configs/conf/stopwords.txt
adjustments
Admitted
because
blood
changes
complained
Discharged
Discussed
Drew
Evaluated
for
hospital
me
medication
of
patient
Performed
Prescribed
Reason
Recommended
Started
tests
The
to
treatment
visit
Visited
was
EOF
</pre>
Next, we download the custom dashboard and start Solr in cloud mode
<pre>
export JAVA_HOME=/usr/lib/jvm/java-1.7.0-openjdk.x86_64
wget "https://raw.githubusercontent.com/vzlatkin/DoctorsNotes/master/other/Chronic%20Symptoms%20(Solr).json" -O /opt/lucidworks-hdpsearch/solr/server/solr-webapp/webapp/banana/app/dashboards/default.json
/opt/lucidworks-hdpsearch/solr/bin/solr start -c -z localhost:2181
/opt/lucidworks-hdpsearch/solr/bin/solr create -c hl7_messages -d data_driven_schema_configs -s 1 -rf 1
</pre>
<h3>5. Create and execute a new NiFi flow</h3>
<ul>
	<li>
	Start by downloading <a href="https://raw.githubusercontent.com/vzlatkin/DoctorsNotes/master/other/Send_HL7_Messages_to_Solr%20%28NiFi%29.xml">this</a> NiFi template to your host machine.
	</li>
	<li>To import the template, open the <a href="http://sandbox.hortonworks.com:9090/nifi/">NiFi UI</a></li>
	<li>Next, open Templates manager:
	<p>
		<img src="/storage/attachments/2467-click-template-button.png">
	</p>
	</li>
	<li>
	<p>
		Click "Browse", then find the template on your local machine, click "Import", and close the Template Window.
	</p>
	<p>
		<img src="/storage/attachments/2468-click-import-button.png">
	</p>
	</li>
	<li>
	<p>
		Drag and drop to instantiate a new template:
	</p>
	<p>
		<img src="/storage/attachments/2469-add-template.png">
	</p>
	</li>
	<li>
	<p>
		Double click the new process group called HL7, and start all of the processes.  To do so, hold down the Shift-key, and select all of the processes on the screen.  Then click the "Start" button:
	</p>
	<p>
		<img src="/storage/attachments/2470-click-the-start-button.png">
	</p>
	<p>
	Here is a quick walk through of the processes starting in the top-left corner.  First, we use ListFile process to get a directory listing from 
		<em>/tmp/hl7-messages</em>.  Second, the FetchFile process reads each file one-by-one, passes the contents to the next step, and deletes if successful.  Third, the text file is parsed as an HL7 formatted message.  Next, the UpdateAttribute and AttributesToJSON processes get the contents ready for insertion into Solr.  Finally, we use the PutSolrContentStream process to add new documents via Solr REST API.  The remaining two processes on the very bottom are for spawning the custom Java code and logging details for troubleshooting.
	</p>
	</li>
</ul>
<h2>Conclusion</h2>
<p>
	Now open the 
	<a href="http://sandbox.hortonworks.com:8983/solr/banana/index.html#/dashboard">Banana UI</a>.  You should see a dashboard that looks similar to the screenshot in the beginning of this article.  You can see how many messages have been processed by clicking the link in the top-right panel called "Filter By".
</p>
<p>
	<img src="/storage/attachments/2481-click-to-filter.png">
</p>