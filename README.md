### Deploying

Prepare your computer and Google Cloud environment according to [Google Cloud Functions Quickstart](https://cloud.google.com/functions/docs/quickstart#before-you-begin)

#### Arch Linux

If you are running Arch Linux execute the following commands to prepare your computer for deployment:

    // install Google Cloud SDK
    pacaur -S google-cloud-sdk
    
    // install alpha components
    sudo gcloud components install alpha
    
    // authenticate
    gcloud auth login
    
    // set default project
    gcloud config set project PROJECT_ID
    
Run `sbt gcDeploy`.
