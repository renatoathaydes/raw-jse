echo "Compiling framework and app"
mkdir -p dist/native
javac -cp "framework/libs/*" framework/src/{HttpServer.java,NativeMain.java} app/src/Starter.java -d dist/native

echo "Creating native image"
native-image -cp "framework/libs/*:dist/native/" NativeMain
