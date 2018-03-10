#!/usr/bin/env bats
# -*- sh -*-

load test_helper

@test "chrome-no-security :none" {
    run lein doo chrome-no-security none-test once
    assert_success
}
