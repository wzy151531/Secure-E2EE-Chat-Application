# Secure E2EE Chat Application

This java project is about a client-server mode and **E2EE** chat program based on socket and **Signal Protocol**.

### Version

Current Client Version: `4.5.3`, Server Version: `4.2.3`

## Alias

The whole project is divided into two parts: server and client. Both projects are built with `gradle6` and `jdk11`.

## Current Function

* Login validation via jdbc.
* Send text message or audio to ~~all connected clients~~ or single connected client.
* Can send some emoji in chat.
* Show current online clients.
* Use TLS connection.
* Show if the message is sent.
* Search chat record at local.
* ~~Clients' text chat data stored in database.~~
* Pairwise E2EE chat between two clients(include text and audio data).
* Save encrypted signal storages at client local.
* Encrypted group chat.
* Save encrypted chatData at local.
* Implement multi-device system prototype.

## TODO List

- [x] Server stores unreceived messages until the receiver is online.
- [x] Save history messages at client local.
- [x] Encrypt all store files and chatData file.
- [x] Implement multi-device system.
- [x] Implement switch device function.
- [ ] Implement asychronized switch info notification.
- [ ] Further encrypted group chat operations(add/leave memebers).
- [ ] ~~Store symmetric encryption key that encrypts the history messages in client OP-TEE.~~
- [ ] History messages backup from old device to new device.
- [ ] Client appends pre keys to database.
- [ ] Client updates signed pre key in database.
- [ ] Update tests

## Quick Start

### Build and run

For server project, first you need to add a `jdbc.properties` file which includes the ssh connection information and database information in `src/main/resources` like:

```bash
sshUser=aaa111
sshPassword=password
dbUser=username
dbPassword=password
```

The two projects both use gradle-wrapper, you can just open either of them in intellij or eclipse simply, and use the gradle tool inside the IDE to build and run the project.

You don't need to install the gradle, just run the following command in the project root folder like `socotra-server` or `socotra-client`:

```bash
./gradlew build
```

and to run the application:

```bash
./gradlew run
```

Or simply use the gradle gui tool inside the IDE(for intellij it's on the right side bar) to run these tasks.

### Junit test

`Junit tests haven't been updated now, it may occur some errors.`

Both two projects use `junit5` to write some tests exclude socket and GUI tests. To run the test, use the gradle tool `test` in menu `Tasks/verification` or use the command in the terminal:

```bash
./gradlew test
```

The test report will automatically generated by gradle in directory `build/reports/tests/test/index.html`.

### Archive and release

For server project, you can release the .jar file, you can archive the server project as:

```bash
./gradlew uploadArchives
```

then the `repos` foler will be generated with serveral .jar files.

For client project, it use `javafx11` to build the GUI, so once you use the gradle to generate a .jar file, you need to download javafx11 sdk first, and run it as following:

```bash
java --module-path $PATHTOJAVAFXSDK11 --add-modules javafx.controls,javafx.fxml,javafx.base -jar $YOURCLIENT.jar
```

## License

[MIT](https://choosealicense.com/licenses/mit/)
