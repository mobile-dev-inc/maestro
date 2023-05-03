echo "Start recording"
xcrun simctl io "$DEVICE_ID" recordVideo --force --codec h264 "$RECORDING_PATH" &
simctlpid=$!

echo "Simctl pid $simctlpid"

# Wait for STDIN to close
cat

echo "Received request to stop recording"

kill -SIGINT "$simctlpid"
wait $simctlpid
