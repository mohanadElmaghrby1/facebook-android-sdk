/**
 * Copyright 2012 Facebook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook;

import org.json.JSONArray;

import android.content.Intent;
import android.os.Bundle;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

// Because TestSession is the component under test here, be careful in calling methods on FacebookTestCase that
// assume TestSession works correctly.
public class TestSessionTests extends FacebookTestCase {
    @SmallTest
    @MediumTest
    @LargeTest
    public void testCanCreateWithPrivateUser() {
        startActivity(new Intent(Intent.ACTION_MAIN), null, null);
        TestSession session = TestSession.createSessionWithPrivateUser(getActivity(), null, null);
        assertTrue(session != null);
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCanCreateWithSharedUser() {
        TestSession session = TestSession.createSessionWithSharedUser(getStartedActivity(), null, null);
        assertTrue(session != null);
    }

    @MediumTest
    @LargeTest
    public void testCanOpenWithSharedUser() throws Throwable {
        final TestBlocker blocker = getTestBlocker();
        TestSession session = getTestSessionWithSharedUser(blocker);

        session.open(getStartedActivity(), new Session.StatusCallback() {
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                assertTrue(exception == null);
                blocker.signal();
            }
        });

        waitAndAssertSuccess(blocker, 1);

        assertTrue(session.getState().getIsOpened());
    }

    @MediumTest
    @LargeTest
    public void testSharedUserDoesntCreateUnnecessaryUsers() throws Throwable {
        final TestBlocker blocker = getTestBlocker();

        TestSession session = getTestSessionWithSharedUser(blocker);
        openSession(getStartedActivity(), session);

        // Note that this test is somewhat brittle in that the count of test users could change for
        // external reasons while the test is running. For that reason it may not be appropriate for an
        // automated test suite, and could be run only when testing changes to TestSession.
        int startingUserCount = countTestUsers();

        session = getTestSessionWithSharedUser(blocker);
        openSession(getStartedActivity(), session);

        int endingUserCount = countTestUsers();

        assertSame(startingUserCount, endingUserCount);
    }

    // This test is currently unreliable, I believe due to timing/replication issues that cause the
    // counts to occasionally be off. Taking out of test runs for now until a more robust test can be added.
    @LargeTest
    public void failing_testPrivateUserIsDeletedOnSessionClose() throws Throwable {
        final TestBlocker blocker = getTestBlocker();

        // See comment above regarding test user count.
        int startingUserCount = countTestUsers();

        TestSession session = getTestSessionWithPrivateUser(blocker);
        openSession(getStartedActivity(), session);

        int sessionOpenUserCount = countTestUsers();

        assertSame(startingUserCount + 1, sessionOpenUserCount);

        session.close();

        int endingUserCount = countTestUsers();

        assertSame(startingUserCount, endingUserCount);
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCannotChangeTestApplicationIdOnceSet() {
        try {
            TestSession.setTestApplicationId("hello");
            TestSession.setTestApplicationId("world");
            fail("expected exception");
        } catch (FacebookException e) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCannotChangeTestApplicationSecretOnceSet() {
        try {
            TestSession.setTestApplicationSecret("hello");
            TestSession.setTestApplicationSecret("world");
            fail("expected exception");
        } catch (FacebookException e) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCannotChangeMachineUniqueUserTagOnceSet() {
        try {
            TestSession.setMachineUniqueUserTag("hello");
            TestSession.setMachineUniqueUserTag("world");
            fail("expected exception");
        } catch (FacebookException e) {
        }
    }

    private int countTestUsers() {
        TestSession session = getTestSessionWithSharedUser(null);

        String appAccessToken = TestSession.getAppAccessToken();
        assertNotNull(appAccessToken);
        String applicationId = session.getApplicationId();
        assertNotNull(applicationId);

        String fqlQuery = String.format("SELECT id FROM test_account WHERE app_id = %s", applicationId);
        Bundle parameters = new Bundle();
        parameters.putString("q", fqlQuery);
        parameters.putString("access_token", appAccessToken);

        Request request = new Request(null, "fql", parameters, null);
        Response response = request.execute();

        JSONArray data = (JSONArray) response.getGraphObject().get("data");
        return data.length();
    }
}
