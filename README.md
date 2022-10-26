# Fast login to pages behind ``login.microsoftonline.com`` for testing

## Purpose
I use this class for speeding up my frontend testing (with selenium, but could be useful for other frameworks aswell).
Especially for testing multiple users with different privileges this login flow saves a lot of time.

## What does it do?
With this class you can login to pages, that use ``login.microsoftonline.com`` with username and password.
If you wonder how to automate UI testing for applications that demand you to login to AAD Accounts this may help you.

This is what te login looks like, that we can skip with the cookies obtained by this cookie fetcher (german version, english looks alike):

![image](https://user-images.githubusercontent.com/43816320/198134062-1d2c1fa0-8cb8-401e-bdd7-070df09817f6.png)

I also saw people (well I also did it myself, until I invested some time to make it better) automating this by using selenum to fill in username, click ``Next``, fill in the password and clicking ``Login``.
Downside of this is that it takes way longer (13.3 seconds versus 3.6 seconds) and felt more brittle. 

## How to use
- Provide the dependencies ``com.squareup.okhttp`` and ``com.squareup.okhttp-urlconnection`` (When writing this readme, I used Version 4.10.0) in your project
- Add the provided class ``LoginCookieFetcher`` to your project (you may rename it and adjust the package name ...)
- Use the static ``login(String url, String username, String password)``-Method of the provided class ``LoginCookieFetcher`` to obtain a List of cookies
- Configure your driver to send all the obtained cookies (e.g. in Selenium with ``driver.manage().addCookie(cookie);``)
