import { CodeSnippet } from "./Examples"

const MockServerInstructions = ({ projectId }: { projectId: string }) => (
  <div className="flex flex-col w-3/4 space-y-4 pt-4">
    <p className="text-lg font-bold">No events found! Follow the guide below to integrate Maestro SDK.</p>
    
    <p className="text-md">First, add the Maestro SDK dependency to your app:</p>
    <CodeSnippet>{`implementation 'dev.mobile:maestro-sdk-android:1.21.4-SNAPSHOT'`}</CodeSnippet>


    <p className="text-md">Then, initialize the Maestro SDK in your app:</p>
    <CodeSnippet>{`MaestroSDK.init('${projectId}')`}</CodeSnippet>
    <p className="text-md">You can always retrieve your project id later by running <span className="italic">maestro mockserver setup</span>.</p>

    <p className="text-md">Next step is to update your app to use the API base url provided by Maestro SDK:</p>
    <CodeSnippet>{`val baseUrl = MaestroSdk.mockServer().url("https://api.yourdomain.com")`}</CodeSnippet>
    <p className="text-md">You can then use <span className="italic">baseUrl</span> as you would normally do. Requests will be sent to <span className="italic">https://mock.mobile.dev</span> and forwarded to your original API if no mock rules are found.</p>

    <p className="text-md">Lastly, deploy a set of rules to the Maestro Mock Server. You can either write your own rules or use the following command to scaffold a sample rule to get started:</p>
    <CodeSnippet>{`maestro mockserver scaffold`}</CodeSnippet>

    <p className="font-semibold">That's it! Now, build and run your app and you should start seeing events come in!</p>
  </div>
)

export default MockServerInstructions