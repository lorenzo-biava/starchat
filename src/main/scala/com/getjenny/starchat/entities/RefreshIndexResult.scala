package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 24/03/17.
  */

case class FailedShard(index_name: String,
                       shard_id: Int,
                       reason: String,
                       status: Int
                       )

case class RefreshIndexResult(failed_shards_n: Int,
                             successful_shards_n: Int,
                             total_shards_n: Int,
                             failed_shards: List[FailedShard]
                             )
