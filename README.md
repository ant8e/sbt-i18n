# sbt i18n

[![Build Status](https://travis-ci.org/ant8e/sbt-i18n.svg?branch=master)](https://travis-ci.org/ant8e/sbt-i18n)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)


An sbt plugin to generate a scala i18n bundle from various sources

## Usage

This plugin requires sbt 1.0.0+

### Testing

Run `test` for regular unit tests.

Run `scripted` for [sbt script tests](http://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html).

### Publishing

1.  publish your source to GitHub
2.  [create a bintray account](https://bintray.com/signup/index) and [set up bintray credentials](https://github.com/sbt/sbt-bintray#publishing)
3.  create a bintray repository `sbt-plugins`
4.  update your bintray publishing settings in `build.sbt`
5.  `sbt publish`
6.  [request inclusion in sbt-plugin-releases](https://bintray.com/sbt/sbt-plugin-releases)
7.  [Add your plugin to the community plugins list](https://github.com/sbt/website#attention-plugin-authors)
8.  [Claim your project an Scaladex](https://github.com/scalacenter/scaladex-contrib#claim-your-project)

## Contribution policy ##

Contributions via GitHub pull requests are gladly accepted from their original author. Along with any pull requests, please state that the contribution is your original work and that you license the work to the project under the project's open source license. Whether or not you state this explicitly, by submitting any copyrighted material via pull request, email, or other means you agree to license the material under the project's open source license and warrant that you have the legal authority to do so.

## License ##

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).
