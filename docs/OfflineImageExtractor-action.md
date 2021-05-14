# Google Cloud Image Extractor Offline

Description
-----------
This action plugin can run offline (asynchronous) detection services to extract enrichments from each image based
on selected features.

Credentials
-----------
If the plugin is run on a Google Cloud Dataproc cluster, the service account key does not need to be
provided and can be set to 'auto-detect'.
Credentials will be automatically read from the cluster environment.

If the plugin is not run on a Dataproc cluster, the path to a service account key must be provided.
The service account key can be found on the Dashboard in the Cloud Platform Console.
Make sure the account key has permission to access Google Cloud Vision.
The service account key file needs to be available on every node in the cluster and
must be readable by all users running the job.

Properties
----------

**Service Account File Path**: Path on the local file system of the service account key used for
authorization. Can be set to 'auto-detect' when running on a Dataproc cluster.
When running on other clusters, the file must be present on every node in the cluster.

**Source Path**: Path to the location of the directory on GCS where the input files are stored.

**Destination Path**: Path to the location of the directory on GCS where output files should be stored.

**Features**: Features to extract from images.

**Batch size**: The max number of responses to put into each output JSON file on Google Cloud Storage. 
The valid range is [1, 100]. If not specified, the default value is 20.

**Language Hints**: Hints to detect the language of the text in the images.

**Aspect Ratios**: Aspect ratios as a decimal number, representing the ratio of the width to the height of the image. 
For example, if the desired aspect ratio is 4/3, the corresponding float value should be 1.33333. 
Only shown when Crop Hints is selected as a feature. If not specified, the best possible crop is returned. 
The number of provided aspect ratios is limited to a maximum of 16; any aspect ratios provided after the 16th are 
ignored.

**Include Geo Results**: Whether to include results derived from the geo information in the image.