---
title: Contribution Guidelines
description: Read the guidelines before setting out to contribute to Peptide on Android.
template: doc
---

This page contains the guidelines for contributing to Peptide on Android. These guidelines are
important to ensure that your contribution is accepted and merged into the main codebase.

:::danger
Make sure you read these guidelines _before starting to contribute_ to Peptide on Android.  
If you do not follow these guidelines, your contribution may be rejected!
:::

## Before You Start

- Make sure you have read
  the [Code of Conduct](https://github.com/peptidechat/.github/blob/master/.github/CODE_OF_CONDUCT.md)
  before contributing. You must follow it at all times.

Before you start contributing, you first need to know what to contribute. Based on that, you must
make a decision:

:::tip[Translation]
If you want to contribute to translations, you can do so by
visiting [Weblate](https://translate.peptide.chat/projects/peptide/android/).
:::

#### I want to fix a bug

If you are fixing a bug, you must follow these guidelines:

- Make sure the bug is reproducible and affects a wide variety of users.
- The root cause of the bug must be the Android app itself, not the server or the API.
- The root cause of the bug must be the Android app itself, not the device.
- If the root cause if a bug is the Android version, make sure any workaround is only used on
  affected versions, while on other versions, the intended behavior is used.

#### I want to add a feature

**All features must be discussed with the app's maintainer before you start working on them!** If
you are adding a feature, you must follow these guidelines:

- The feature must be useful to a wide variety of users.
- UI/UX is centrally managed by Peptide's design team. Make sure your feature aligns closely with the
  current design as well as [Material 3](https://m3.material.io/).
- If you need any new UI, you are recommended to request a design from the design team and implement
  it as per the design. If your UI/UX is not up to the mark, your PR may be rejected by the design
  team.
- The feature must not be a duplicate of an existing feature.
- If you are adding a new feature, you should consult with the Android team at Peptide to make sure
  that the feature is not already in development or planned for the future. Conflicting features
  will not be merged.

## In General, ...

- Use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) for your commit
  messages. This helps in generating changelogs and tracking changes.
- Make sure your code is easy to understand. You need not document every line of code, but if
  something is not immediately obvious to a seasoned Android developer, you should add a comment.
- Make sure your code is well-formatted. You can use Android Studio's built-in code formatter to
  format your code.
- Make sure your code 'fits in' with the rest of the codebase.
- Make sure your code is well-tested. You should write unit tests for your code if applicable.

## If you came this far...

Let's get started! You may continue to the [Development Setup](/android/contributing/setup) guide to
set up your development environment.
