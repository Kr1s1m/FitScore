
create table accounts(
    account_id uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    account_date_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    account_email character varying(255),
    account_username character varying(255),
    account_age smallint,
    account_height smallint,
    account_weight numeric(4,1)
);

create table posts(
    post_id uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    account_id uuid  NOT NULL REFERENCES accounts(account_id),
    post_date_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    post_date_updated timestamp without time zone,
    post_title character varying(100),
    post_body text
);

create table replies(
    reply_id uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    account_id uuid NOT NULL REFERENCES accounts(account_id),
    post_id uuid NOT NULL REFERENCES posts(post_id),
    reply_parent_id uuid REFERENCES replies(reply_id),
    reply_date_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    reply_date_updated timestamp without time zone,
    reply_body text
);

create table verification_tokens(
    verification_token_id uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    account_id uuid NOT NULL REFERENCES accounts(account_id),
    verification_token_expiration_date timestamp without time zone,
    verification_token_token character varying(255)
);

create type vote_type as enum ('up', 'down');

create table post_votes(
    post_vote_id uuid uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    account_id uuid NOT NULL REFERENCES accounts(account_id),
    post_id uuid NOT NULL REFERENCES posts(post_id),
    post_vote_vote_type vote_type
);

create table reply_votes(
    reply_vote_id uuid uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    account_id uuid NOT NULL REFERENCES accounts(account_id),
    reply_id uuid NOT NULL REFERENCES posts(post_id),
    reply_vote_vote_type vote_type
);

create type access_type as enum ('user', 'admin');

create table roles(
    role_id smallint NOT NULL DEFAULT 1,
    role_access_type access_type NOT NULL DEFAULT 'user'::access_type
);

insert into roles(
    role_id,
	role_access_type
) values (
    0,
	'user'::access_type,
);

insert into roles(
    role_id,
	role_access_type
) values (
    1,
	'admin'::access_type,
);

insert into accounts(
	account_email,
	account_username,
	account_age,
	account_height,
	account_weight
) values (
	'dero@fitscore.com',
	'Dero',
	25,
	164,
	67.4
);

insert into accounts(
	account_email,
	account_username,
	account_age,
	account_height,
	account_weight
) values (
	'kr1s1m@fitscore.com',
	'Kr1s1m',
	25,
	173,
	54.5
);