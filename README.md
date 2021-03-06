`slackstatus` is a script for setting your Slack status. It's built on top
of [Lumo](https://github.com/anmonteiro/lumo/), a cross-platform tool for running
ClojureSript on Node.

# Installation

## Required Lumo version

`slackstatus` requires Lumo 1.8.0 or later.

## Getting dependencies with Leiningen

1. Install Leiningen
1. `lein deps` in this repo

## Getting dependencies without Leiningen

There may be a nice way to do this with the new Clojure CLI tools, or
possibly with newer Lumo versions. PRs welcome!

## Getting a token to access the Slack API

Interacting with Slack's API requires you to generate a token on their
website. The easiest way seems to
be
[legacy tokens](https://api.slack.com/custom-integrations/legacy-tokens).

Once you have a token, save it in a file called `~/.slacktoken`.

### What if legacy tokens get deprecated?

Slack has a bunch of documentation about the "right" way to build apps
now, and it seems like a lot of work for a tiny script. If there's an
easy way to do it the way they want, it would be great to migrate.

# Usage

The first argument you pass to the script will be your status
text. Wrap it in quotes if there are any spaces.

The second argument is an optional emoji. If no emoji is specified,
Slack defaults to a text bubble.

## The easy, but noisy, way

```lumo -c $(lein classpath) slackstatus.cljs "desired status" desired_emoji```

While this approach works, it will generate a few hundred WARNING
messages that I haven't figured out how to suppress.

If anyone knows how to hide those, a PR would be awesome!

## The annoying, but quiet, way

It turns out that `lein classpath` will bring in a bunch of stuff that
is either already provided by Lumo, or which isn't necessary. By
manually constructing the classpath, we can avoid the deps which are
causing the WARNING messages. The minimal classpath is just
`com.cemerick/url` plus its dependency `pathetic`. If you're run `lein
deps` in this directory, you'll have them installed. Then to use the script:

```lumo -c ~/.m2/repository/com/cemerick/url/0.1.1/url-0.1.1.jar:~/.m2/repository/pathetic/pathetic/0.5.0/pathetic-0.5.0.jar slackstatus.cljs "desired status" desired_emoji```
