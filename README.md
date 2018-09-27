# Codice Maven Generic Utilities
Repository of re-usable Maven utilities.

### Modules
The following modules are defined:
* jacoco
 
#### jacoco
Provides extensions for the Jacoco maven plugin. 

[`LenientLimit`](jacoco/src/main/java/org/codice/jacoco/LenientLimit.java) provides an extension to Jacoco's standard limit implementation which is more lenient with respect to the minimum and maximum limits to which a value is compared against. 
This should help account for small differences in Java compiler and OS as these can be generating different bytecodes.

System properties supported:
* `offsetJacoco` - specifies the offset to apply to the minimum and maximum values read from pom files in order to be lenient about the comparison. Defaults to 0.02.
* `computeJacoco` - forces the limit minimum and maximum values as such that Jacoco's comparison will fail and yield warnings that includes the new computed values. This can be useful when one wants to update the pom files after having made changes to the source code or to the test cases.

### Future iterations
Future implementations will:
* Provide CI/CD support
