Changelog
==========

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