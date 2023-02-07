import { CodeSnippet } from "./Examples"

const MockServerInstructions = ({ projectId }: { projectId: string }) => (
  <>
    <p className="text-lg font-bold">No events found! Follow the guide below to integrate Maestro SDK.</p>
    
    <p className="text-md">First, add the Maestro SDK dependency to your app:</p>
    <CodeSnippet>{`implementation 'dev.mobile:maestro-sdk-android:1.21.4-SNAPSHOT'`}</CodeSnippet>


    <p className="text-md">Then, initialize the Maestro SDK in your app:</p>
    <CodeSnippet>{`MaestroSDK.init('${projectId}')`}</CodeSnippet>

    <p className="text-md">Next step is to update your app to use the API base url provided by Maestro SDK:</p>
    <CodeSnippet>{`val baseUrl = MaestroSdk.mockServer().url("https://api.yourdomain.com")`}</CodeSnippet>
    <p className="text-md">You can then use <span className="italic">baseUrl</span> as you would normally do. Requests will be sent to <span className="italic">https://mock.mobile.dev</span> and forwarded to your original API if no mock rules are found.</p>

    <p className="text-md">Lastly, deploy a set of rules to the Maestro Mock Server. Below is a sample <span className="italic">index.js</span> file that you can use to get started:</p>
    <CodeSnippet>{`// .maestro/mockserver/index.js\n\nget('/some-route', (req, res) => {\n\tres.json({\n\t\tmocked: true\n\t});\n});`}</CodeSnippet>
    <p className="text-md">Run the following command to deploy it:</p>
    <CodeSnippet>{`maestro mockserver deploy <path_to_workspace>`}</CodeSnippet>

    <p className="font-semibold">That's it! Now, build and run your app and you should start seeing events come in!</p>
  </>
)

export default MockServerInstructions