Changelog
==========

Version 1.0.1 (07.07.2017)
--------------------------
####Client

* Fix a NPE when calling `Interactor.close()`.
* Throw `IOException` when calling `Interactor.submitRequests()` or `Interactor.nextResponse()` on a closed `Interactor` instance.
* Fix the non-respected read and write timeouts when using `Call`, `MultiCall` or `Interactor` objects.

####Test
* Supress and fix compiler warnings in some of the test utility classes.

Version 1.0.0 (09.06.2017)
--------------------------
* Initial release.