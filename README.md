# Flight 627

  Project "Flight 627" is prototype work to evaluate possible future cloud-based developer
  tooling.

  The underlying idea is based on a Git-aware project sync mechanism (you can think of it as
  DropBox for code) that keeps projects in sync across different machines and services. While the
  storage and syncing mechanism is cloud-based, clients to this mechanism can be anything (ranging
  from a plugin for Eclipse, a file watcher process on your machine, a browser application, a
  headless service running somewhere else in the cloud, etc.).

## The basic design

  The fundamental design idea behind this project is the differentiation between the resources of
  the projects and additional services that can work on those resources.

  The resources are usually the artifacts of your project (everything that is or would be committed
  into a Git repo). The backbone sync mechanism stores those resources and helps clients to keep track
  of changes (so that syncing is possible).

  The services can act as clients of this resource sync backbone and operate on those synced resources.
  The nature of those clients can be very different from each other. Examples include:

  * Eclipse plugin to automatically sync projects in your workspace with the sync service that is running in the cloud
  * a file watcher process that does the same as the Eclipse plugin, but for files on your disc
  * an IntelliJ plugin that does the same as the Eclipse plugin, but plugs into IntelliJ
  * a web editor that allows you to edit and save resources using a browser only
  * a service that compiles Java resources and stores compilation results (like errors and warnings) back into the cloud as metadata
  * a service that runs JUnit tests automatically whenever a resource changes
  * a service that keeps a search index up-to-date
  * much more...

  The underlying mechanisms are language neutral. Specific services can provide different services for
  different languages or language versions.

## The Eclipse plugin

  The Eclipse plugin allows you to sync the projects in your workspace with the cloud-based sync backbone
  mechanism. It is the central element to provide a smooth and seamless transition from using Eclipse towards
  using more and more cloud-based tooling.

  At the moment the Eclipse plugin also acts as a service that reacts to changes and stores metadata (compilation
  and reconciling results) back into the cloud. It can also answer content-assist requests.

## The web editor

  The current web editor is a prototype to allow users to edit synced projects using a browser only. The editor
  listens to metadata changes and displays them while typing and provides content-assist for Java by sending
  a content-asists request into the cloud and by reacting to the first response to this request.

  The editor is implemented based on the Eclipse Orion editor component at the moment.

## Technical background

  The current focus of the prototype work is to figure out what is possible to realize on top of this design
  and what makes sense to develop further.

  The sync backbone provides a RESTful API to access and change resources and metadata. Additional communication
  (announcing changes, sending around metadata) is implemented using WebSockets.

## Status

  This is prototype work and by no means meant to be used in production. It misses important features, good
  error handling and a persistent storage on the backend.

## License

  Not yet defined

