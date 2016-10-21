# Node.js Translator app Overview

The Node.js translator app provides required REST implementation for translating a content.

## Run the app in Bluemix

1. Create a cf app in Bluemix
2. Bind the IBM Language Translator service to the app
3. cf push this app 
4. Access the running app in a browser at http://<app route>/translate?content=<conent to translate>&transLang=<language to be translated>

