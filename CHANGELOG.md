Changelog
==========

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