#!/usr/bin/env bats
# -*- sh -*-

load test_helper

@test "nashorn :advanced" {
    run lein doo nashorn advanced once
    assert_success
}

@test "nashorn :advanced fail" {
    run lein doo nashorn advanced-fail once
    assert_failure
}
