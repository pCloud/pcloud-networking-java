Changelog
==========

Version 1.2.3 (21.08.2017)
--------------------------

* The only change is the change of the Maven `groupId` property from `pcloud-networking-java` to `com.pcloud.pcloud-networking-java`. Users wanting to use this version or any other futures will need to update their dependencies list.

For Gradle users

```groovy
	dependencies {
    	...
    	compile "pcloud-networking-java:composer:1.2.2"
    }
```

becomes

```groovy
	dependencies {
    	...
    	compile "com.pcloud.pcloud-networking-java:composer:1.2.3"
    }
```

Version 1.2.2 (17.08.2017)
--------------------------

#### Composer
* Fix a bug leading to `NullPointerException` errors when calling no-arg methods of objects returned by `ApiComposer.compose()`.


Version 1.2.1 (15.08.2017)
--------------------------

#### Client
* Add a missing getter in `PCloudAPIClient` for the `EndpointProvider` supplied by `PCloudAPIClient.Builder.endpointProvider(EndpointProvider)` method.

Version 1.2.0 (15.08.2017)
--------------------------

#### Client

* The ability ot set an `EndpointProvider` has been moved one level below in the abstraction layers and now can be set to a `PCloudAPIClient` instance via the `PCloudAPIClient.Builder.endpointProvider()` method.
See the example below on how to migrate from the previous version:

```java

EndpointProvider myEndpointProvider = new ...;

// Building an ApiComposer instance with custom EndpointProvider as of version 1.1.0 and below:
ApiComposer composer = new APiComposer.Builder()
	.endpointProvider(myEndpointProvider)
    .build();


// Setting a custom EndpointProvider for version 1.2.0 and above:
PCloudAPIClient client = PCloudAPIClient.newClient()
	.endpointProvider(myEndpointProvider)
    .create();

ApiComposer composer = new APiComposer.Builder()
	.apiClient(client)
    .build();
```
* `EndpointProvider.endpointConnectionError()` will now be properly called for errors during connection initiation and for all reads/writes.


* `Request.endpoint()` can now return a null `Endpoint` if none or null has been set via `Request.Builder.endpoint(Endpoint)`.

* `Call` instances created via `PCloudAPIClient.newCall(Request)` for `Request` objects without an explictly set `Endpoint`, will be done to an endpoint returned by the supplied or default `EndpointProiver` instance.

* `MultiCall` instances can now created for a user-specified `Endpoint` via the `PCloudAPIClient.newCall(List<Request>, Endpoint)` method. Previous behavior was to pick up the `Endpoint` set to the first `Request` instance from the request list.

* Fixes for potential race conditions leading to NPEs when cancelling/closing `Call` and `MultiCall` instances too early.

#### Composer

* The ApiComposer.Builder.endpointProvider() method has been removed (see the related changes in the `Client` module).

Version 1.1.0 (24.07.2017)
--------------------------

#### Transformer

* Add the `Transformer.Builder().addTypeAlias()` method for adding type aliases. The method can be used to register concrete implementations of interface types such as in the example below:

```java
interface Model {
...
}

class DefaultModel implements Model {
...
}

Transformer transformer = Transformer.create()
		...
		.addTypeAlias(Model.class, DefaultModel.class)
        ...
    	.build();
```

Version 1.0.5 (21.07.2017)
--------------------------

#### Client

* Fix a bug where `PCloudAPIClient` instances sharing a common `ConnectionPool` having different timeout settings would use recycled connections with timeouts different from the ones set via `PCLoudAPIClient.Builder`. The changes are entirely internal.


Version 1.0.4 (19.07.2017)
--------------------------
This is a maintenance release with updated documentation and removed unused classes

#### Utils

* Remove the unused `ByteCountingSource` from the `utils` module.
* Move `FixedLengthSource` from `utils` to the `binapi-client` module

#### Composer-Adapter-RxJava

* Remove `MultiCallProducer` and `MultiCallOnSubscribe` as they are not used anymore.

Version 1.0.3 (18.07.2017)
--------------------------
#### Composer

* Adapted interface methods will always use `Endpoint.DEFAULT` as endpoint, to leave the opportunity to have the final endpoint resolved from the `PCloudAPIClient`'s `EndpointProvider` instance.


Version 1.0.2 (11.07.2017)
--------------------------
#### Client

* Remove the akward ConnectException thrown from RealConnection.connect(), the method will pass-through any thrown IOExceptions in its body.

Version 1.0.1 (07.07.2017)
--------------------------
#### Client

* Fix a NPE when calling `Interactor.close()`.
* Throw `IOException` when calling `Interactor.submitRequests()` or `Interactor.nextResponse()` on a closed `Interactor` instance.
* Fix the non-respected read and write timeouts when using `Call`, `MultiCall` or `Interactor` objects.

#### Test
* Supress and fix compiler warnings in some of the test utility classes.

Version 1.0.0 (09.06.2017)
--------------------------
* Initial release.