package az.theternal.samples.data

/** What the samples are built from; the playground has no DI. */
class SampleDependencies(
    val auth: AuthRepository = FakeAuthRepository(),
    val users: UserRepository = FakeUserRepository(),
    val posts: PostRepository = FakePostRepository(),
)
